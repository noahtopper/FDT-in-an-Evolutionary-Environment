// Noah Topper
// FDT in an Evolutionary Environment, 2019
// Tests FDT in the Transparent Newcomb Problem, competing against CDT.

import java.io.*;
import java.util.*;

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

public class NewcombsProblem
{
   // Initial population rates.
   static final double CDT = 1;
   static final double FDT = 0;
   // Prediction rate
   static double P = 0.99;
   // Payoffs
   static int HIGH = 10000;
   static int LOW = 1000;
   // Misc parameters
   static final int NUM_AGENTS = 3000;
   static final int NUM_GENERATIONS = 200;
   static final int NUM_ROUNDS = 100;
   static final double DEATH_RATE = 0.01;
   static final double MUTATION_RATE = 0.001;
	static final double DISPLAY_RATE = 100;

   // This function will set the payoffs to random integers, w/ HIGH > LOW.
   // It will also set P to be between 0.5 and 1.
   public static void randomize()
   {
      Random rand = new Random();
      TreeSet<Integer> randomInts = new TreeSet<>();

		// Generates a two random integers from 1 to 1,000,000
      while (randomInts.size() < 2)
         randomInts.add(rand.nextInt(1000000) + 1);

      Iterator<Integer> iterator = randomInts.iterator();
      LOW = iterator.next();
      HIGH = iterator.next();
      // P = 0;
      // while (P <= 0.5)
      //    P = Math.random();
   }

   // Displays the current state of the population this generation
   public static void displayPopulation(double[] popRates, int gen)
   {
		// Displays population every DISPLAY_RATE generations.
      if ((gen + 1) % DISPLAY_RATE != 0)
         return;
      System.out.println("Generation " + (gen+1));
      System.out.println("=================================");
      System.out.println("Proportion of CDT: " + popRates[0]);
      System.out.println("Proportion of FDT: " + popRates[1]);
      System.out.println();
   }

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

	// Based on the earned utilities of the agents, repopulate the population.
   // Eliminate low utility agents, reproduce high utility agents, mutate,
   // modify the population rates, and return the new population.
   public static int[] repopulate (int[] population, double[] popRates, double[] utilities)
   {
      int death = (int)(DEATH_RATE * NUM_AGENTS);
      int[] agents = new int[2]; // Track the population changes here.
      double sum = 0, min = Double.MAX_VALUE;

		for (int i = 0; i < NUM_AGENTS; i++)
			agents[population[i]]++;

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
			while (agents[old] == 0)
				old = population[indices[index++]];
			int mutant = (int)(Math.random() * 2);
			agents[old]--;
			agents[mutant]++;
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
         agents[born]++;
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
			int dead = population[utilsMap.next()];;
			while (agents[dead] == 0)
				dead = population[utilsMap.next()];
			agents[dead]--;
		}

      // Mark the changes in the population.
      for (int i = 0; i < 2; i++)
         popRates[i] = (double)agents[i] / NUM_AGENTS;
      return setPopulation(popRates);
   }

   public static void faceoff(int[] population, double[] utilities, int k)
   {
      int type = population[k], pred = prediction(type);

      // CDT agent
      if (type == 0)
      {
         // If the predictor thought they'd two-box, they get the low reward.
         if (pred == 2)
            utilities[k] += LOW;
         // Otherwise, they get both!
         else
            utilities[k] += (HIGH + LOW);
      }
      // FDT agent
      else
      {
         // If the predictor thought they'd two-box, they get the low reward.
         if (pred == 2)
            utilities[k] += LOW;
         // Otherwise, if FDT decides to one-box, it gets the high reward.
         // If FDT decides to two-box, it gets both!
         else
         {
            if (FDT() == 1)
               utilities[k] += HIGH;
            else
               utilities[k] += (HIGH + LOW);
         }
      }
   }

   // Randomly selects the prediciton made by the predictor.
   public static int prediction(int type)
   {
      double rand = Math.random();
      // If player is a CDT agent
      if (type == 0)
      {
         // Return correct prediction with probability P, incorrect otherwise.
         return (0 <= rand && rand < P) ? 2 : 1;
      }
      // If player is an FDT agent
      else
      {
         // Return correct prediction with probability P, incorrect otherwise.
         if (0 <= rand && rand < P)
            return FDT();
         else
            return (FDT() == 1) ? 2 : 1;
      }
   }

   // Determines FDT's action if both boxes are full,
   // given the payoffs and prediction strength.
   public static int FDT()
   {
      // FDT's utility calculation.
      double one = P * HIGH + (1 - P) * LOW;
      double two = (1 - P) * (HIGH + LOW) + P * LOW;
      return (one > two) ? 1 : 2;
   }

   // Takes in the current intended population rates, returns an array
   // representing the population of agents and their types.
   public static int[] setPopulation(double[] popRates)
   {
      int[] population = new int[NUM_AGENTS];

      // Set the population to have a number of each agent
      // proportional to their population rates.
      int i, cdt = (int)(popRates[0] * NUM_AGENTS);
      for(i = 0; i < cdt; i++)
         population[i] = 0;

      int j, fdt = (int)(popRates[1] * NUM_AGENTS);
      for(j = i; j < i + fdt; j++)
         population[j] = 1;

      // Fill in any missing spots randomly,
      // proportional to the intended population rates.
      while (j < NUM_AGENTS)
      {
         double rand = Math.random();
         if (0 <= rand && rand < popRates[0])
         {
            cdt++;
            population[j++] = 0;
         }
         else
         {
            fdt++;
            population[j++] = 1;
         }
      }
      // Correct population rate for randomness
      popRates[0] = (double)cdt / NUM_AGENTS;
      popRates[1] = (double)fdt / NUM_AGENTS;

      return population;
   }

   public static void main(String[] args)
   {
      double[] popRates = {CDT, FDT};
      int[] population = setPopulation(popRates);

      // If uncommented, this line of code will randomly set the payoff values
      // and prediction accuracy. Otherwise, they maintain their default values.
      // randomize();

      displayPopulation(popRates, -1);
      for (int i = 0; i < NUM_GENERATIONS; i++)
      {
         displayPopulation(popRates, i);
         double[] utilities = new double[NUM_AGENTS];

         for (int j = 0; j < NUM_ROUNDS; j++)
            for (int k = 0; k < NUM_AGENTS; k++)
               faceoff(population, utilities, k);

         population = repopulate(population, popRates, utilities);
      }
   }
}
