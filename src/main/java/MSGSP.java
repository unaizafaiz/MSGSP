import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class MSGSP {
	ArrayList<List> sequenceCollection = new ArrayList<>();
	HashMap<Integer, Float> parameters = new HashMap<>();
	float sdc_value;

	MSGSP(ArrayList<List> sequenceCollection, HashMap<Integer, Float> parameters, float sdc_value){
		this.sequenceCollection = sequenceCollection;
		this.parameters = sortByValue(parameters); //sorted MIS values
        this.sdc_value = sdc_value;

		//Finding 1-itemsets
		List<Integer> itemSetCollection = find_1_ItemSet(sequenceCollection);


		//Get support count for each itemset
		Map<Integer, Integer> supportCount = find_1_itemsetSupport(itemSetCollection, sequenceCollection);

		//Finding L
		List<Integer> l = findLSet(supportCount);



		try {
			PrintWriter writer = new PrintWriter("./output/result.txt","UTF-8");

			//Finding F1
			ArrayList<List<List>> frequentItemset = findF1(l, supportCount, writer);

			//Generating candidate sequence and frequent itemsets until F(k-1) = null
			for(int k =2; !frequentItemset.isEmpty();k++){
				ArrayList<List<List>> candidateSequence = new ArrayList<>();
				MSCandidateGenSPM msCandidateGenSPM = new MSCandidateGenSPM(parameters,supportCount,sdc_value,sequenceCollection.size());
				if(k==2){
					//Find candidates for second level
					candidateSequence = findC2(l, supportCount, parameters,sdc_value,sequenceCollection);

				}else {
				    //Find candidate for Ck
					candidateSequence.clear();
					msCandidateGenSPM.join(frequentItemset);
					candidateSequence.addAll(msCandidateGenSPM.prune(frequentItemset));
				}

				//Finding the frequent itemsets
				HashMap<List<List>,Integer> itemSetSupport = findSupportCount(candidateSequence,sequenceCollection);
				frequentItemset.clear();
				for(List<List> itemset: candidateSequence){
					int minIndex = msCandidateGenSPM.getMinIndex(itemset,parameters);
					Integer minMISItem = msCandidateGenSPM.getItem(itemset,minIndex);
					if(itemSetSupport.containsKey(itemset)) {
						float support = (float) itemSetSupport.get(itemset) / sequenceCollection.size();
						if (parameters.get(minMISItem) <= support)
							frequentItemset.add(itemset);
					}
				}
                if(!frequentItemset.isEmpty()) {
                    printToFile(frequentItemset, k, writer, itemSetSupport);
                }
			}
			writer.close();

		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

    /**
     * Printing patterns to a file
     * @param frequentItemset
     * @param k
     * @param writer
     * @param itemSetSupport
     */
	private void printToFile(ArrayList<List<List>> frequentItemset, int k, PrintWriter writer, HashMap<List<List>, Integer> itemSetSupport) {
		writer.println("The number of "+k+" sequential patterns is "+frequentItemset.size());
		for(List<List> sequence:frequentItemset){
            String frequentItemSequence ="";
			for(List<Integer> itemset: sequence) {
                frequentItemSequence += "{";
				for (int i=0; i<itemset.size();i++) {
					frequentItemSequence += itemset.get(i);
					if(i<itemset.size()-1) {
						frequentItemSequence+=",";
					}
				}
				frequentItemSequence += "}";
            }
            writer.println("Pattern: <"+frequentItemSequence+">: Count = " + itemSetSupport.get(sequence));
        }
    }

	/**
	 * Finding all frequent 1-itemsets from L
	 * @param l
	 * @param supportCount
	 * @param writer
	 */
	private ArrayList<List<List>> findF1(List<Integer> l, Map<Integer, Integer> supportCount, PrintWriter writer) {
		List<Integer> f1 = new ArrayList<>();
		for(Integer i: l){
			if(supportCount.containsKey(i)) {
				float support = (float) supportCount.get(i) / sequenceCollection.size();
				if (parameters.get(i) <= support)
					f1.add(i);
			}
		}

		ArrayList<List<List>> frequentItemset = new ArrayList<>();
		writer.println("The number of 1 sequential patterns is "+f1.size());
		for(Integer i: f1){
			List<List> sequence = new ArrayList<>();
			List<Integer> itemset = new ArrayList<>();
			itemset.add(i);
			sequence.add(itemset);
			frequentItemset.add(sequence);
			writer.println("Pattern: <{"+i+"}>: Count = "+supportCount.get(i));
		}

		return frequentItemset;

	}

	/**
	 * Getting the L set for init-pass()
	 * @param supportCount
	 * @return
	 */
	private List<Integer> findLSet(Map<Integer, Integer> supportCount) {
		List<Integer> theLSet = new ArrayList<>();
		Integer item1 = 0;
		boolean firstItem  = false;
		for(Integer key: parameters.keySet()){
            if(supportCount.containsKey(key)) {
                float support = (float) supportCount.get(key) / sequenceCollection.size();
                if (parameters.get(key) <= support && !firstItem) {
                    theLSet.add(key);
                    item1 = key;
                    firstItem = true;
                } else if (firstItem && parameters.get(item1) <= support) {
                    theLSet.add(key);
                }
            }

		}
		return theLSet;
	}

	/**
	 * Finding all possible unique items in the itemsets
	 * @param sequenceCollection
	 * @return
	 */
	private List<Integer> find_1_ItemSet(ArrayList<List> sequenceCollection) {
		List<Integer> newItemSet = new ArrayList<>();

		for (List<List> sequence:
			sequenceCollection) {
			for (List<Integer> itemset:
				sequence) {
				for (Integer item:
					itemset) {
					if (!newItemSet.contains(item)) {
						newItemSet.add(item);
					}
				}
			}

		}
		return newItemSet;

	}

	/**
	 * Finding the support count for a single item itemset
	 * @param itemSetCollection
	 * @param sequenceCollection
	 * @return
	 */
	private Map<Integer, Integer> find_1_itemsetSupport(List<Integer> itemSetCollection, ArrayList<List> sequenceCollection) {
		HashMap<Integer, Integer> supportCount = new HashMap<>();
		for(Integer newItem: itemSetCollection){
			for (List<List> sequence:
				sequenceCollection) {
				boolean sequenceContainsItem = false;
				for (List<Integer> itemSet:
					sequence) {
					for(Object item: itemSet){
						if (item.equals(newItem)) {
							sequenceContainsItem = true;
							if (supportCount.containsKey(newItem)) {
								supportCount.put(newItem, supportCount.get(newItem) + 1);
							} else {
								supportCount.put(newItem, 1);

							}
							break;

						}
					}
					if(sequenceContainsItem){
						break;
					}

				}

			}

		}

		//To sort the map by key
		Map<Integer, Integer> map = new TreeMap<>(supportCount);
		return map;

	}


	/**
	 * Finding the support count of all the itemsets in a candidate sequence list
	 * @param candidateSequenceList
	 * @param sequenceCollection
	 * @return
	 */
	private HashMap<List<List>,Integer> findSupportCount(ArrayList<List<List>> candidateSequenceList, ArrayList<List> sequenceCollection) {
		HashMap<List<List>, Integer> supportCount = new HashMap<>();

		for(List<List> candidateSequence: candidateSequenceList) {
			for (List<List> sequence : sequenceCollection) {
				int i = 0;
				for (List<Integer> itemset : sequence) {
					if (i < candidateSequence.size()) {
						if (itemset.containsAll(candidateSequence.get(i))) {
							i++;
						}
					}
				}
				if (i == candidateSequence.size()) {
					if (supportCount.containsKey(candidateSequence)) {
						supportCount.put(candidateSequence, supportCount.get(candidateSequence) + 1);
					} else {
						supportCount.put(candidateSequence, 1);
					}
				}
			}
		}

		return supportCount;

	}

	/**
	 * Sorting parameters by the value
	 * @param parameters
	 * @return sorted HashMap of parameters
	 */
	private HashMap<Integer,Float> sortByValue(HashMap<Integer, Float> parameters) {
		HashMap<Integer, Float> sortedParameters = parameters
				.entrySet()
				.stream()
				.sorted(comparingByValue())
				.collect(
						toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
								LinkedHashMap::new));

		return sortedParameters;
	}

	/**
	 * Returns an arraylist of candidate keys of level 2.
	 * @param l,supportCount, parameters, sdc_value 
	 * @return ArrayList of candidate keys.
	 */

	private ArrayList<List<List>> findC2(List<Integer> l, Map<Integer, Integer> supportCount, HashMap<Integer, Float> parameters, Float sdc_value, ArrayList<List> sequenceCollection) {
		int count =0;
		ArrayList<List<List>> candidatesLevel2 = new ArrayList<List<List>>();

		for (int i = 0; i < l.size()-1; i++) {
			//find the supportcount of an element in the List l.
			float supportCountElement1 = ((float)supportCount.get( l.get(i) )) / sequenceCollection.size();

			//Check whether the supportcount is greater than the specified MIS Value in the parameter file.
			//Note: This condition should be checked before considering the second value, ie starting the second loop.
			//If this fails, go to the next element in ith loop.

			if( supportCountElement1 >= parameters.get(l.get(i))) {

				//Add {x},{x}
				List<Integer> b0 = new ArrayList<>();
				b0.add(l.get(i));
				List<Integer> c0 = new ArrayList<>();
				c0.add(l.get(i));
				List<List> level2Candidate0 = new ArrayList<>();
				level2Candidate0.add(b0);
				level2Candidate0.add(c0);
                candidatesLevel2.add(level2Candidate0);
                for (int j=i+1; j< l.size(); j++) {
					float supportCountElement2 = ((float)supportCount.get( l.get(j) )) / sequenceCollection.size();
					//Comparing the difference in support count with the sdc_value from parameter file.
					if( (  supportCountElement2 >= parameters.get(l.get(i)) &&
							(Math.abs( supportCountElement2 - supportCountElement1 ) <= sdc_value))){


						//Adds {x,y}
						List<Integer> a = new ArrayList<>();
						a.add(l.get(i));
						a.add(l.get(j));
						Collections.sort(a);
						List<List> level2Candidate1 = new ArrayList<>();
						level2Candidate1.add(a);

						//Adds {x},{y}
						List<Integer> b = new ArrayList<>();
						b.add(l.get(i));
						List<Integer> c = new ArrayList<>();
						c.add(l.get(j));
						List<List> level2Candidate2 = new ArrayList<>();
						level2Candidate2.add(b);
						level2Candidate2.add(c);

						//Add {y},{x}
						List<Integer> b1 = new ArrayList<>();
						b1.add(l.get(j));
						List<Integer> c1 = new ArrayList<>();
						c1.add(l.get(i));
						List<List> level2Candidate3 = new ArrayList<>();
						level2Candidate3.add(b1);
						level2Candidate3.add(c1);

						candidatesLevel2.add(level2Candidate1);
						candidatesLevel2.add(level2Candidate2);
						candidatesLevel2.add(level2Candidate3);
						count ++;
					}
				}
			}
		}
		return candidatesLevel2;
	}

}
