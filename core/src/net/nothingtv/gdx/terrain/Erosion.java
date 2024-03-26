package net.nothingtv.gdx.terrain;

import java.util.Random;

// java port of https://github.com/SebLague/Hydraulic-Erosion/blob/master/Assets/Scripts/Erosion.cs
public class Erosion {

    public int seed;
    public int erosionRadius = 3;
    public float inertia = .05f; // At zero, water will instantly change direction to flow downhill. At 1, water will never change direction.
    public float sedimentCapacityFactor = 4; // Multiplier for how much sediment a droplet can carry
    public float minSedimentCapacity = .01f; // Used to prevent carry capacity getting too close to zero on flatter terrain
    public float erodeSpeed = .3f;
    public float depositSpeed = .3f;
    public float evaporateSpeed = .01f;
    public float gravity = 4;
    public int maxDropletLifetime = 30;
    private Random prng;

    public float initialWaterVolume = 1;
    public float initialSpeed = 1;

    // Indices and weights of erosion brush precomputed for every node
    int[][] erosionBrushIndices;
    float[][] erosionBrushWeights;

    int currentSeed;
    int currentErosionRadius;
    int currentMapSize;

    // Initialization creates a System.Random object and precomputes indices and weights of erosion brush
    void Initialize (int mapSize, boolean resetSeed) {
        if (resetSeed || prng == null || currentSeed != seed) {
            prng = new Random(seed);
            currentSeed = seed;
        }

        if (erosionBrushIndices == null || currentErosionRadius != erosionRadius || currentMapSize != mapSize) {
            InitializeBrushIndices (mapSize, erosionRadius);
            currentErosionRadius = erosionRadius;
            currentMapSize = mapSize;
        }
    }

    private boolean checkMapValidity(float[] map, String comment) {
        int invalidPoints = 0;
        for (int i = 0; i < map.length; i++)
            if (Float.isNaN(map[i]) || Float.isInfinite(map[i])) {
                invalidPoints++;
            }
        if (invalidPoints > 0) {
            System.out.printf("%s: Found %d invalid height points%n", comment, invalidPoints);
            return false;
        }
        return true;
    }

    public void Erode(float[] map, int mapSize) {
        Erode(map, mapSize, 1, false);
    }

    public void Erode (float[] map, int mapSize, int numIterations, boolean resetSeed) {
        Initialize (mapSize, resetSeed);

        checkMapValidity(map, "Begin");
        for (int iteration = 0; iteration < numIterations; iteration++) {
            // Create water droplet at random point on map
            float posX = prng.nextFloat (0, mapSize - 1);
            float posY = prng.nextFloat (0, mapSize - 1);
            float dirX = 0;
            float dirY = 0;
            float speed = initialSpeed;
            float water = initialWaterVolume;
            float sediment = 0;

            for (int lifetime = 0; lifetime < maxDropletLifetime; lifetime++) {
                int nodeX = (int) posX;
                int nodeY = (int) posY;
                int dropletIndex = nodeY * mapSize + nodeX;
                // Calculate droplet's offset inside the cell (0,0) = at NW node, (1,1) = at SE node
                float cellOffsetX = posX - nodeX;
                float cellOffsetY = posY - nodeY;

                // Calculate droplet's height and direction of flow with bilinear interpolation of surrounding heights
                HeightAndGradient heightAndGradient = CalculateHeightAndGradient (map, mapSize, posX, posY);

                // Update the droplet's direction and position (move position 1 unit regardless of speed)
                dirX = (dirX * inertia - heightAndGradient.gradientX * (1 - inertia));
                dirY = (dirY * inertia - heightAndGradient.gradientY * (1 - inertia));
                // Normalize direction
                float len = (float)Math.sqrt (dirX * dirX + dirY * dirY);
                if (len != 0) {
                    dirX /= len;
                    dirY /= len;
                }
                posX += dirX;
                posY += dirY;

                // Stop simulating droplet if it's not moving or has flowed over edge of map
                if ((dirX == 0 && dirY == 0) || posX < 0 || posX >= mapSize - 1 || posY < 0 || posY >= mapSize - 1) {
                    break;
                }

                // Find the droplet's new height and calculate the deltaHeight
                float newHeight = CalculateHeightAndGradient (map, mapSize, posX, posY).height;
                float deltaHeight = newHeight - heightAndGradient.height;

                // Calculate the droplet's sediment capacity (higher when moving fast down a slope and contains lots of water)
                float sedimentCapacity = Math.max (-deltaHeight * speed * water * sedimentCapacityFactor, minSedimentCapacity);

                // If carrying more sediment than capacity, or if flowing uphill:
                if (sediment > sedimentCapacity || deltaHeight > 0) {
                    // If moving uphill (deltaHeight > 0) try fill up to the current height, otherwise deposit a fraction of the excess sediment
                    float amountToDeposit = (deltaHeight > 0) ? Math.min (deltaHeight, sediment) : (sediment - sedimentCapacity) * depositSpeed;
                    sediment -= amountToDeposit;

                    // Add the sediment to the four nodes of the current cell using bilinear interpolation
                    // Deposition is not distributed over a radius (like erosion) so that it can fill small pits
                    map[dropletIndex] += amountToDeposit * (1 - cellOffsetX) * (1 - cellOffsetY);
                    map[dropletIndex + 1] += amountToDeposit * cellOffsetX * (1 - cellOffsetY);
                    map[dropletIndex + mapSize] += amountToDeposit * (1 - cellOffsetX) * cellOffsetY;
                    map[dropletIndex + mapSize + 1] += amountToDeposit * cellOffsetX * cellOffsetY;

                    //checkMapValidity(map, "b1 iteration " + iteration + ", lifetime " + lifetime);
                } else {
                    // Erode a fraction of the droplet's current carry capacity.
                    // Clamp the erosion to the change in height so that it doesn't dig a hole in the terrain behind the droplet
                    float amountToErode = Math.min ((sedimentCapacity - sediment) * erodeSpeed, -deltaHeight);

                    // Use erosion brush to erode from all nodes inside the droplet's erosion radius
                    for (int brushPointIndex = 0; brushPointIndex < erosionBrushIndices[dropletIndex].length; brushPointIndex++) {
                        int nodeIndex = erosionBrushIndices[dropletIndex][brushPointIndex];
                        float weighedErodeAmount = amountToErode * erosionBrushWeights[dropletIndex][brushPointIndex];
                        float deltaSediment = (map[nodeIndex] < weighedErodeAmount) ? map[nodeIndex] : weighedErodeAmount;
                        map[nodeIndex] -= deltaSediment;
                        sediment += deltaSediment;
//                        if (!checkMapValidity(map, "b2 iteration " + iteration + ", lifetime " + lifetime))
//                            System.out.println("First occurrence of an invalid point");
                    }
                }

                // Update droplet's speed and water content
                speed = (float)Math.sqrt (speed * speed + Math.max(0, deltaHeight) * gravity);
                water *= (1 - evaporateSpeed);
            }
        }
    }

    HeightAndGradient CalculateHeightAndGradient (float[] nodes, int mapSize, float posX, float posY) {
        int coordX = (int) posX;
        int coordY = (int) posY;

        // Calculate droplet's offset inside the cell (0,0) = at NW node, (1,1) = at SE node
        float x = posX - coordX;
        float y = posY - coordY;

        // Calculate heights of the four nodes of the droplet's cell
        int nodeIndexNW = coordY * mapSize + coordX;
        float heightNW = nodes[nodeIndexNW];
        float heightNE = nodes[nodeIndexNW + 1];
        float heightSW = nodes[nodeIndexNW + mapSize];
        float heightSE = nodes[nodeIndexNW + mapSize + 1];

        // Calculate droplet's direction of flow with bilinear interpolation of height difference along the edges
        float gradientX = (heightNE - heightNW) * (1 - y) + (heightSE - heightSW) * y;
        float gradientY = (heightSW - heightNW) * (1 - x) + (heightSE - heightNE) * x;

        // Calculate height with bilinear interpolation of the heights of the nodes of the cell
        float height = heightNW * (1 - x) * (1 - y) + heightNE * x * (1 - y) + heightSW * (1 - x) * y + heightSE * x * y;

        return new HeightAndGradient (height, gradientX, gradientY);
    }

    void InitializeBrushIndices (int mapSize, int radius) {
        erosionBrushIndices = new int[mapSize * mapSize][];
        erosionBrushWeights = new float[mapSize * mapSize][];

        int[] xOffsets = new int[radius * radius * 4];
        int[] yOffsets = new int[radius * radius * 4];
        float[] weights = new float[radius * radius * 4];
        float weightSum = 0;
        int addIndex = 0;

        for (int i = 0; i < erosionBrushIndices.length; i++) {
            int centreX = i % mapSize;
            int centreY = i / mapSize;

            if (centreY <= radius || centreY >= mapSize - radius || centreX <= radius + 1 || centreX >= mapSize - radius) {
                weightSum = 0;
                addIndex = 0;
                for (int y = -radius; y <= radius; y++) {
                    for (int x = -radius; x <= radius; x++) {
                        float sqrDst = x * x + y * y;
                        if (sqrDst < radius * radius) {
                            int coordX = centreX + x;
                            int coordY = centreY + y;

                            if (coordX >= 0 && coordX < mapSize && coordY >= 0 && coordY < mapSize) {
                                float weight = 1 - (float)Math.sqrt (sqrDst) / radius;
                                weightSum += weight;
                                weights[addIndex] = weight;
                                xOffsets[addIndex] = x;
                                yOffsets[addIndex] = y;
                                addIndex++;
                            }
                        }
                    }
                }
            }

            int numEntries = addIndex;
            erosionBrushIndices[i] = new int[numEntries];
            erosionBrushWeights[i] = new float[numEntries];

            for (int j = 0; j < numEntries; j++) {
                erosionBrushIndices[i][j] = (yOffsets[j] + centreY) * mapSize + xOffsets[j] + centreX;
                if (weightSum > 0)
                    erosionBrushWeights[i][j] = weights[j] / weightSum;
                else
                    erosionBrushWeights[i][j] = 0;
            }
        }
    }

    static class HeightAndGradient {
        public float height;
        public float gradientX;
        public float gradientY;

        public HeightAndGradient(float height, float gradientX, float gradientY) {
            this.height = height;
            this.gradientX = gradientX;
            this.gradientY = gradientY;
        }
    }
}