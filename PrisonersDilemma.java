// Noah Topper
// FDT in an Evolutionary Environment, 2019
// Tests FDT in the Prisoner's Dilemma, competing against CDT and Cooperators.

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

public class PrisonersDilemma
{
   // Initial population rates
   static final double DEF = (double)1/3;
   static final double COOP = (double)1/3;
   static final double FDT = (double)1/3;
   // Signal strength
   static final double P = 0.9;
   // Game payoffs: L < D < C < W
   static int L = 1;
   static int D = 4;
   static int C = 7;
   static int W = 10;
   // Misc paramters
   static final int NUM_AGENTS = 10000; // must be even
   static final int NUM_GENERATIONS = 1000;
   static final int NUM_ROUNDS = 100;
   static final double DEATH_RATE = 0.01;
   static final double MUTATION_RATE = 0.001;
	static final double DISPLAY_RATE = 100;

   // Calling this function will set the payoffs to four random values
   // from -1000 to +1000, while still constituting a Prisoner's Dilemma.
   public static void randomizePayoffs()
   {
      Random rand = new Random();
      TreeSet<Integer> randomInts = new TreeSet<>();

      while (randomInts.size() < 4)
         randomInts.add(rand.nextInt(999) + 1);

      Iterator<Integer> iterator = randomInts.iterator();
      L = iterator.next();
      D = iterator.next();
      C = iterator.next();
      W = iterator.next();
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
		int[] agents = new int[3]; // Track the population changes here.
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
			int mutant = (int)(Math.random() * 3);
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
		for (int i = 0; i < 3; i++)
			popRates[i] = (double)agents[i] / NUM_AGENTS;
		return setPopulation(popRates);
	}

   // Generates random signal based on opponent's type.
   // Correct with probability P, incorrect with probability 1 - P
   public static int receiveSignal(int type)
   {
      double rand = Math.random(), mass = (1 - P) / 2;
      int signal;

      // Signals the correct type with probability P, and the
      // chance of error is split evenly between the other two types
      if (0 <= rand && rand < P)
         signal = type;
      else if (P <= rand && rand < (P + mass))
         signal = (type + 1) % 3;
      else
         signal = (type + 2) % 3;

      return signal;
   }

   // Two agents faceoff in a Prisoner's Dilemma. Based in their types,
   // we determine what happens and save their utility earned.
   public static void faceoff
   (int[] population, double[] utilities, char[] fdt, int x, int y)
   {
      int type1 = population[x], type2 = population[y];

      // The first agent is FDT
      if (type1 == 2)
      {
         // The signal and fdt policy determine the agent's action
         char action1 = fdt[receiveSignal(type2)];

         // FDT vs. Defector
         if (type2 == 0)
         {
            utilities[x] += (action1 == 'c') ? L : D;
            utilities[y] += (action1 == 'c') ? W : D;
         }
         // FDT vs. Cooperator
         else if (type2 == 1)
         {
            utilities[x] += (action1 == 'c') ? C : W;
            utilities[y] += (action1 == 'c') ? C : L;
         }
         // FDT vs. FDT
         else
         {
            char action2 = fdt[receiveSignal(type1)];
            // If they output the same action, both cooperate or both defect
            if (action1 == action2)
            {
               utilities[x] += (action1 == 'c') ? C : D;
               utilities[y] += (action1 == 'c') ? C : D;
            }
            // If they output different actions, one "wins" and one "loses"
            else
            {
               utilities[x] += (action1 == 'c') ? L : W;
               utilities[y] += (action1 == 'c') ? W : L;
            }
         }
      }
      // Only the second agent is FDT
      else if (type2 == 2)
      {
         char action = fdt[receiveSignal(type1)];
         // FDT vs. Defector
         if (type1 == 0)
         {
            utilities[y] += (action == 'c') ? L : D;
            utilities[x] += (action == 'c') ? W : D;
         }
         // FDT vs. Cooperator
         else
         {
            utilities[y] += (action == 'c') ? C : W;
            utilities[x] += (action == 'c') ? C : L;
         }
      }
      // Both agents are Defectors or Cooperators
      else if (type1 == type2)
      {
         utilities[x] += (type1 == 0) ? D : C;
         utilities[y] += (type1 == 0) ? D : C;
      }
      // One agent is a Defector and one is a Cooperator
      else
      {
         utilities[x] += (type1 == 0) ? W : L;
         utilities[y] += (type1 == 0) ? L : W;
      }
   }

   // Returns an array where each index i represents the probability
   // of the opponent being type i, given the signal and base rates.
   public static double[] bayes(double[] popRates, int signal)
   {
      // Set up likelihood vector according to the signal.
      double[] likelihood = {(1-P)/2, (1-P)/2, (1-P)/2};
      likelihood[signal] = P;

      double sum = 0;
      double[] probability = new double[3];
      // Multiplies the prior odds vector by the likelihood vector
      for (int i = 0; i < 3; i++)
      {
         probability[i] = likelihood[i] * popRates[i];
         sum += probability[i];
      }
      // Convert the odds to probabilities
      for (int i = 0; i < 3; i++)
         probability[i] /= sum;

      return probability;
   }

   // Calculates the expected utility of an FDT agent's action
   // given each signal, the current population, and the payoffs.
   public static char[] FDT(double[] popRates)
   {
      // probability[i][j] represents the probability,
      // given signal i, that the opponent is of type j
      double[][] probability = new double [3][];
      for (int i = 0; i < 3; i++)
      {
         probability[i] = bayes(popRates, i);
         // System.out.println(Arrays.toString(probability[i]));
      }
      // euCooperate[i] represents the expected utility
      // of cooperating, given signal i
      double[] euCooperate = new double[3];
      double[] euDefect = new double[3];
      // Adds up the expected utilities of cooperating or defecting
      // against a coopertor or defector.
      for (int i = 0; i < 3; i++)
      {
         euCooperate[i] += probability[i][0] * L;
         euCooperate[i] += probability[i][1] * C;
         euDefect[i] += probability[i][0] * D;
         euDefect[i] += probability[i][1] * W;
      }
      // When calculating the expected utility of cooperating given signal i,
      // if we're up against another FDT agent, we assume they output the same
      // result if they got the same signal.
      for (int i = 0; i < 2; i++)
      {
         euCooperate[i] += probability[i][2] * ((1-P) / 2) * C;
         euDefect[i] += probability[i][2] * ((1-P) / 2) * D;
      }
      euCooperate[2] += probability[2][2] * P * C;
      euDefect[2] += probability[2][2] * P * D;

      // We're still missing the expected utilities of cooperating or defecting
      // against another FDT agent when they receive a different signal.
      // We calculate these by trial and error, trying different settings of
      // FDT's action given signal i, and outputing a stable setting.
      char[] fdt = {'d', 'd', 'd'};
      for (int i = 0; i < 8; i++)
      {
         boolean swap = false;
         double[] euC = euCooperate.clone();
         double[] euD = euDefect.clone();

         if (fdt[0] == fdt[1])
         {
            // Read: "Add to my expected utility of cooperating given signal 2,
            // the probability that my opponent is FDT, times the probability
            // that they incorrectly detect me as Defector or Cooperator, times
            // the approporiate utility values."
            euC[2] += probability[2][2] * (1-P) * (fdt[0] == 'c' ? C : L);
            euD[2] += probability[2][2] * (1-P) * (fdt[0] == 'c' ? W : D);
         }
         else
         {
            euC[2] += probability[2][2] * ((1-P) / 2) * (C + L);
            euD[2] += probability[2][2] * ((1-P) / 2) * (W + D);
         }

         if (fdt[0] == fdt[2])
         {
            euC[1] += probability[1][2] * (1+P) * (fdt[0] == 'c' ? C : L);
            euD[1] += probability[1][2] * (1+P) * (fdt[0] == 'c' ? W : D);
         }
         else
         {
            euC[1] += probability[1][2] * ((1-P) / 2) * (fdt[0] == 'c' ? C : L);
            euC[1] += probability[1][2] * P * (fdt[2] == 'd' ? L : C);
            euD[1] += probability[1][2] * ((1-P) / 2) * (fdt[0] == 'c' ? W : D);
            euD[1] += probability[1][2] * P * (fdt[2] == 'd' ? D : W);
         }

         if (fdt[1] == fdt[2])
         {
            euC[0] += probability[0][2] * (1+P) * (fdt[1] == 'c' ? C : L);
            euD[0] += probability[0][2] * (1+P) * (fdt[1] == 'c' ? W : D);
         }
         else
         {
            euC[0] += probability[0][2] * ((1-P) / 2) * (fdt[1] == 'c' ? C : L);
            euC[0] += probability[0][2] * P * (fdt[2] == 'd' ? L : C);
            euD[0] += probability[0][2] * ((1-P) / 2) * (fdt[1] == 'c' ? W : D);
            euD[0] += probability[0][2] * P * (fdt[2] == 'd' ? D : W);
         }

         // Check if current setting is valid. If so, break out of the loop.
         for (int j = 0; j < 3; j++)
         {
            if (euC[j] > euD[j] && fdt[j] == 'd')
            {
               swap = true;
               fdt[j] = 'c';
            }
            else if (euC[j] <= euD[j] && fdt[j] == 'c')
            {
               swap = true;
               fdt[j] = 'd';
            }
         }
         if (!swap)
            break;
      }
      return fdt;
   }

   // Displays the current state of the population this generation
   public static void displayPopulation(double[] popRates, int gen)
   {
      // Displays population every DISPLAY_RATE generations.
      if ((gen + 1) % DISPLAY_RATE != 0)
         return;
      System.out.println("Generation " + (gen+1));
      System.out.println("=================================");
      System.out.println("Proportion of Defectors: " + popRates[0]);
      System.out.println("Proportion of Cooperators: " + popRates[1]);
      System.out.println("Proportion of FDT Agents: " + popRates[2]);
      System.out.println();
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

      int j, cooperators = (int)(popRates[1] * NUM_AGENTS);
      for(j = i; j < i + cooperators; j++)
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
            cooperators++;
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
      popRates[1] = (double)cooperators / NUM_AGENTS;
      popRates[2] = (double)fdt / NUM_AGENTS;

      return population;
   }

   public static void main(String[] args)
   {
      // Initialize our population rates and the population itself.
      double[] popRates = {DEF, COOP, FDT};
      int[] population = setPopulation(popRates);
      int[] indices = new int[NUM_AGENTS];
      for (int i = 0; i < NUM_AGENTS; i++)
         indices[i] = i;

      // If uncommented, this line of code will randomly set the payoff values.
      // Otherwise, they maintain their default values.
      // randomizePayoffs();

      displayPopulation(popRates, -1);
      // Pair off and repopulate agents for NUM_GENERATIONS
      for (int i = 0; i < NUM_GENERATIONS; i++)
      {
			displayPopulation(popRates, i);
         double[] utilities = new double[NUM_AGENTS];
         char[] fdt = FDT(popRates);

         // Have random agents face off for NUM_ROUNDS and add up their utilities
         for (int j = 0; j < NUM_ROUNDS; j++)
         {
            shuffleArray(indices);
            // Have two agents from random indices faceoff.
            // Add their earned utilities to their respective indices.
            for (int k = 0; k < NUM_AGENTS; k += 2)
               faceoff(population, utilities, fdt, indices[k], indices[k+1]);
         }
         population = repopulate(population, popRates, utilities);
      }
   }
}
