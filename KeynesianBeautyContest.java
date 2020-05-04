import java.io.*;
import java.util.*;
import java.awt.Point;

// Sets up an object to perform weighted random selection on a set of integers
class RandomCollection
{
    private final NavigableMap<Double, Integer> map = new TreeMap<>();
    private final Random random = new Random();
    private double total = 0;

    public void add(double weight, Integer result)
    {
      if (weight < 0)
         return;
      total += weight;
      map.put(total, result);
    }

    public Integer next()
    {
      double value = random.nextDouble() * total;
      return map.higherEntry(value).getValue();
    }
}

public class KeynesianBeautyContest
{
	// Initial population rates
	static final double CDT = (double)1/3;
	static final double RAND = (double)1/3;
	static final double FDT = (double)1/3;
	// Track the frequency of each agent. Useful in this game.
	static int[] agentCounts = new int[3];
	// Misc parameters
	static final double FRAC = (double)2/3;
   	static final int NUM_AGENTS = 10000;
   	static final int NUM_GENERATIONS = 100000;
   	static final int NUM_ROUNDS = 100;
   	static final double DEATH_RATE = 0.01;
   	static final double MUTATION_RATE = 0.001;
	static final double DISPLAY_RATE = 1000;

	// Randomly shuffles the values in an array.
	public static void shuffleArray(int[] array)
	{
		Random random = new Random();
		for (int i = array.length - 1; i > 0; i--)
		{
			int index = random.nextInt(array.length);
			int temp = array[index];
			array[index] = array[i];
			array[i] = temp;
		}
	}

	// Calculates the determinant of a 2x2 matrix.
	public static double determinant(double[][] matrix)
	{
		return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
	}

	// Performs Cramers Rule to solve for CDT's value.
	public static double cdt(double[] popRates)
	{
		double[][] matrix1 = new double[2][2];
		double[][] matrix2 = new double[2][2];

		// Equations derived from those given in the paper.
		matrix1[0][0] = (popRates[1] / (popRates[1] + popRates[2])) * FRAC * 50;
		matrix1[0][1] = -1 * (popRates[2] / (popRates[1] + popRates[2])) * FRAC;
		matrix1[1][0] = popRates[1] * FRAC * 50;
		matrix1[1][1] = 1 - popRates[2] * FRAC;

		matrix2[0][0] = 1;
		matrix2[0][1] = -1 * (popRates[2] / (popRates[1] + popRates[2])) * FRAC;
		matrix2[1][0] = -1 * popRates[0] * FRAC;
		matrix2[1][1] = 1 - popRates[2] * FRAC;

		return determinant(matrix1) / determinant(matrix2);
	}

	// Performs Cramers Rule to solve for FDT's value.
	public static double fdt(double[] popRates)
	{
		double[][] matrix1 = new double[2][2];
		double[][] matrix2 = new double[2][2];

		// Equations from those given in the paper.
		matrix1[0][0] = 1;
		matrix1[0][1] = (popRates[1] / (popRates[1] + popRates[2])) * FRAC * 50;
		matrix1[1][0] = -1 * popRates[0] * FRAC;
		matrix1[1][1] = popRates[1] * FRAC * 50;

		matrix2[0][0] = 1;
		matrix2[0][1] = -1 * (popRates[2] / (popRates[1] + popRates[2])) * FRAC;
		matrix2[1][0] = -1 * popRates[0] * FRAC;
		matrix2[1][1] = 1 - popRates[2] * FRAC;

		return determinant(matrix1) / determinant(matrix2);
	}

	// Takes in the current intended population rates, returns an array
   // representing the population of agents and their types.
   public static int[] setPopulation(double[] popRates)
   {
      int[] population = new int[NUM_AGENTS];

      // Set the population to have a number of each agent
      // proportional to their population rates.
      int i, defectors = (int)(popRates[0] * NUM_AGENTS);
      for(i = 0; i < defectors; i++)
         population[i] = 0;

      int j, random = (int)(popRates[1] * NUM_AGENTS);
      for(j = i; j < i + random; j++)
         population[j] = 1;

      int k, fdt = (int)(popRates[2] * NUM_AGENTS);
      for(k = j; k < j + fdt; k++)
         population[k] = 2;

      // Fill in any missing spots randomly,
      // proportional to the intended population rates.
      while (k < NUM_AGENTS)
      {
         double rand = Math.random();
         if (0 <= rand && rand < popRates[0])
         {
            defectors++;
            population[k++] = 0;
         }
         else if (popRates[0] <= rand && rand < (popRates[0] + popRates[1]))
         {
            random++;
            population[k++] = 1;
         }
         else
         {
            fdt++;
            population[k++] = 2;
         }
      }
      // Correct population rate for randomness
      popRates[0] = (double)defectors / NUM_AGENTS;
      popRates[1] = (double)random / NUM_AGENTS;
      popRates[2] = (double)fdt / NUM_AGENTS;

		agentCounts[0] = defectors;
		agentCounts[1] = random;
		agentCounts[2] = fdt;

      return population;
   }

	// Displays the current state of the population this generation
   public static void displayPopulation(double[] popRates, int gen, double cdt, double fdt)
   {
		// Displays population every DISPLAY_RATE generations.
      if ((gen + 1) % DISPLAY_RATE != 0)
         return;
		System.out.println("CDT: " + cdt(popRates));
		System.out.println("FDT: " + fdt(popRates));
      System.out.println("Generation " + (gen+1));
      System.out.println("=================================");
      System.out.println("Proportion of CDT: " + popRates[0]);
      System.out.println("Proportion of Random: " + popRates[1]);
      System.out.println("Proportion of FDT: " + popRates[2]);
      System.out.println();
   }

	public static double utility(double avg, double guess)
	{
		double error = Math.abs(FRAC * avg - guess);
		if (error == 0 || 1 / error > 1000)
			return 1000;
		return 1 / error;
	}

	public static void faceoff(double cdt, double fdt, int[] population, double[] utilities)
	{
		double avg = 0;
		double[] randomGuesses = new double[agentCounts[1]];

		// Track all the random guesses.
		for (int i = 0; i < agentCounts[1]; i++)
		{
			double guess = Math.random() * 100;
			randomGuesses[i] = guess;
			avg += guess;
		}
		// Add up the guesses of all the CDT and FDT agents.
		avg += cdt * agentCounts[0];
		avg += fdt * agentCounts[2];
		avg /= NUM_AGENTS;

		int index = 0;
		for (int i = 0; i < NUM_AGENTS; i++)
		{
			if (population[i] == 0)
				utilities[i] += utility(avg, cdt);
			else if (population[i] == 1)
				utilities[i] += utility(avg, randomGuesses[index]);
			else
				utilities[i] += utility(avg, fdt);
		}
	}

	// Based on the earned utilities of the agents, repopulate the population.
	// Eliminate low utility agents, reproduce high utility agents, mutate,
	// modify the population rates, and return the new population.
	public static int[] repopulate (int[] population, double[] popRates, double[] utilities)
	{
		int death = (int)(DEATH_RATE * NUM_AGENTS);
		double sum = 0, min = Double.MAX_VALUE;

		// Mutate a small random subset of the population to random types.
		int mutation = (int)(MUTATION_RATE * NUM_AGENTS);
		int[] indices = new int[NUM_AGENTS];
		for (int i = 0; i < NUM_AGENTS; i++)
			indices[i] = i;
		shuffleArray(indices);
		int index = 0;
		for (int i = 0; i < mutation; i++)
		{
			int old = population[indices[index++]];
			while (agentCounts[old] == 0)
				old = population[indices[index++]];
			int mutant = (int)(Math.random() * 3);
			agentCounts[old]--;
			agentCounts[mutant]++;
		}

		// RandomCollection allows us to perform a random selection of an index,
		// weighted by the utility earned by the corresponding agent.
		RandomCollection utilsMap = new RandomCollection();
		for (int i = 0; i < NUM_AGENTS; i++)
			utilsMap.add(utilities[i], i);
		// Randomly choose a set of high-utility agents,
		// and add more of them to the population.
		for (int i = 0; i < death; i++)
		{
			int born = population[utilsMap.next()];
			agentCounts[born]++;
		}

		// RandomCollection that selects agents proportional
		// to how *low* their earned utility is.
		for (int i = 0; i < NUM_AGENTS; i++)
			utilities[i] = 1 / utilities[i];
		utilsMap = new RandomCollection();
		for (int i = 0; i < NUM_AGENTS; i++)
			utilsMap.add(utilities[i], i);
		// Randomly choose a set of low-utility agents, and kill them off.
		for (int i = 0; i < death; i++)
		{
			int dead = population[utilsMap.next()];

			while (agentCounts[dead] == 0)
				dead = population[utilsMap.next()];

			agentCounts[dead]--;
		}

		// Mark the changes in the population.
		for (int i = 0; i < 3; i++)
			popRates[i] = (double)agentCounts[i] / NUM_AGENTS;
		return setPopulation(popRates);
	}

	public static void main(String[] args)
   {
      double[] popRates = {CDT, RAND, FDT};
      int[] population = setPopulation(popRates);

      displayPopulation(popRates, -1, 50, 50);
      for (int i = 0; i < NUM_GENERATIONS; i++)
      {
         double[] utilities = new double[NUM_AGENTS];
			double cdt = cdt(popRates);
			double fdt = fdt(popRates);
			displayPopulation(popRates, i, cdt, fdt);

         for (int j = 0; j < NUM_ROUNDS; j++)
             faceoff(cdt, fdt, population, utilities);

         population = repopulate(population, popRates, utilities);
      }
   }
}
