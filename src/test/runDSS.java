package test;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import sample.Data;


public class runDSS {
	static String input_path = "D:\\Data\\FPdataset\\";
	static LinkedList<Data> sampleList = new LinkedList<Data>(); 
	static Data data;
	static HashMap<Integer,Integer> localHist = new HashMap<Integer,Integer>(); // localHist
	static HashMap<Integer,Integer> globalHist = new HashMap<Integer,Integer>();
	static BufferedReader br;
	static long startTime;
	static long calcDistTime = 0;
	static long rankingTime = 0;
	static String line;
	static int[] tran;
	static int num = 0;
	static int r_count = 0;
	
//	static int sampleSize = 5;
//	static int R = 1;
//	static String dataset = "simple";
	static int sampleSize = 5000;
	static int R = 10;
	static String dataset = "retail";  //kosarak  retail  retail_10K  BMS1_10K   BMS2_10K 
	 //simple     BMS1  BMS2  T10I4D100K
	static String inputFile = input_path + dataset + ".txt";
//	static TreeMap<Integer,Integer> map = new TreeMap<Integer,Integer>();
	
	
	public static void main(String[] args) throws IOException {
		/* read data fill into sample and intialize histogram */
		br = new BufferedReader(new FileReader(inputFile));
		for(int i = 0 ; i < sampleSize ; i++){
			System.out.println("******* Line: " + ++num + " ********");
			line = br.readLine();
			tran = transferLineToArray(line); // get the transaction int[]
			sampleList.add(new Data(tran, 0)); // add a Data in the reservoir
			insertTranIntoHist(globalHist, tran);
			insertTranIntoHist(localHist, tran);
		}
		
		/* rank the initial S by leave-one-out method */
		// calculate each transaction's distance and update distance in Data
		rankDataInSample(sampleList,globalHist);
		
//		printSampleListInfo(sampleList);
//		printMapContent(globalHist);
//		printMapContent(localHist);
		
		/* process others transactions */
		while ((line = br.readLine()) != null) {
			System.out.println("******* Line: " + ++num + " ********");
			tran = transferLineToArray(line);
			insertTranIntoHist(globalHist, tran); // new item insert into global histogram
//			System.out.println("before re-ranking : ");
//			printSampleListInfo(sampleList);
			determineWhetherInsertNewTran(tran,sampleList,globalHist,localHist); // determine by distance with or without Tnew
			if(r_count >= R){
				rankDataInSample(sampleList,globalHist); // rank sample by distance
				r_count = 0;
			}		
//			printMapContent(globalHist);
//			System.out.println("after re-ranking : ");
//			printSampleListInfo(sampleList);
		}
		
//		printSampleListInfoHead(sampleList,100);
		printSampleListInfo(sampleList);
	}
	
	/** determine if it needs to insert Tnew **/
	@SuppressWarnings("unchecked")
	public static void determineWhetherInsertNewTran(int[] tran,LinkedList<Data> sampleList, 
			HashMap<Integer,Integer> globalHist, HashMap<Integer,Integer> localHist){
		double dist_withoutTnew = calculateDistance(localHist, sampleList.size(), globalHist);
//		System.out.println("dist_withoutTnew = " + dist_withoutTnew);
//		LinkedList<Data> replacedList = (LinkedList<Data>) sampleList.clone(); // clone a list
//		replaced.removeLast(); // remove LRT (LRT means the most insignificant data)
		HashMap<Integer,Integer> replacedHist = (HashMap<Integer, Integer>) localHist.clone();
		removeTranIntoHist(replacedHist, sampleList.getLast().getTran());
		insertTranIntoHist(replacedHist, tran);
		double dist_withTnew = calculateDistance(replacedHist, sampleList.size(), globalHist);
//		System.out.println("dist_withTnew : " + dist_withTnew);
		
		// determine which is better, if true, repalce it and update local histogram
		if(dist_withoutTnew > dist_withTnew){ // replace LRT : remove LRT and insert new transaction to sample
			r_count++; // record times of replace, inorder to know when to re-rank
			Data newData = new Data(tran,dist_withoutTnew); // new transaction
//			System.out.println("dist_withoutTnew = " + dist_withoutTnew);
			// replace LRT by new Data and udpate local histogram
			removeTranIntoHist(localHist, sampleList.removeLast().getTran()); // remove LRT and update local histogram
			insertTranIntoHist(localHist, newData.getTran()); // update local histogram because adding new tran
			int index = positionOfTnewInsert(sampleList,newData); // insert new data to ranked sample
			if(index != -1)// insert new data in appropriate position
				sampleList.add(index, newData); // insert in the middle
			else
				sampleList.add(newData); // insert in the last
//			System.out.println("replace !!!");
//			System.out.println("local histogram : ");
//			printMapContent(localHist);
//			printSampleListInfo(sampleList);
		}
		
		
		
		
	}
	
	/** find index of trans **/
	public static int positionOfTnewInsert(LinkedList<Data> sampleList, Data newData){
		for(int i=0; i < sampleList.size(); i++){
			if(newData.getDistance() > sampleList.get(i).getDistance()){
				return i; // index founded
			}
		}
		return -1; // it need to insert in the last
		
	}
	
	
	/** rank data in sample : including re-calculating distance and sorting **/ 
	@SuppressWarnings("unchecked")
	public static void rankDataInSample(LinkedList<Data> sampleList, HashMap<Integer,Integer> globalHist){
		/* calculate distance of each Data in sample */
//		printSampleListInfo(sampleList);
		startTime = System.currentTimeMillis();
		System.out.println("re-ranking...");
		double distance;
		HashMap<Integer,Integer> leaveOneOutHist;
		for(int i = 0; i < sampleList.size(); i++){
			leaveOneOutHist = (HashMap<Integer, Integer>) localHist.clone(); // in order to use leave-one-out
			removeTranIntoHist(leaveOneOutHist, sampleList.get(i).getTran()); // remove some items by single transaction
			distance = calculateDistance(leaveOneOutHist, sampleList.size() - 1 , globalHist); // calculate distance
			sampleList.get(i).setDistance(distance); //update the distance value in sample List
		}
		calcDistTime += System.currentTimeMillis() - startTime;
//		System.out.println("before : ");
//		printSampleListInfo(sampleList);	
		
		startTime = System.currentTimeMillis();
		/* rank the sample by distance by descending order*/
		Collections.sort(sampleList, new Comparator<Data>() {
			public int compare(Data d1, Data d2){
				if(d1.getDistance() - d2.getDistance() > 0)
					return -1;
				else if(d1.getDistance() - d2.getDistance() < 0)
					return 1;
				else
					return 0;
			}
		});
		rankingTime += System.currentTimeMillis() - startTime;
//		System.out.println("after : ");
//		printSampleListInfo(sampleList);
		System.out.println("re-ranking done!!!");
		System.out.println("calcDistTime : " + calcDistTime / 1000 + " s");
		System.out.println("rankingTime : " + rankingTime / 1000 + " s");
	}
	
	/** calculate distance by leave on out **/
	public static double calculateDistance(HashMap<Integer,Integer> localHist, int size, HashMap<Integer,Integer> globalHist){
		/* sum up distance of each item in global histogram */
		double supSample, supDs;
		double distance = 0;
		// sum up
		for(Integer i : globalHist.keySet()){
			if(localHist.get(i) != null)
				supSample = (double) localHist.get(i) / size; 
			else
				supSample = 0.0; // not exist in sample
			supDs = (double) globalHist.get(i) / num; // support of item in data stream (histogram)
			distance += Math.pow(supSample - supDs, 2);
		}
		return distance;
	}
	
	/** calculate distance by leave on out **/
//	public static double calculateDistance(LinkedList<Data> sampleList, HashMap<Integer,Integer> globalHist){
//		/* sum up distance of each item in global histogram */
//		double supSample, supDs;
//		double distance = 0;
//		// sum up
//		for(Integer i : globalHist.keySet()){
//			supSample = (double) getFreqOfItemInSample(i,sampleList) / sampleList.size(); // support of item in sample 
//			supDs = (double) globalHist.get(i) / num; // support of item in data stream (histogram)
//			distance += Math.pow(supSample - supDs, 2);
//		}
//		return distance;
//	}
	
	/** get support of the item in sample (support means relative frequency) **/
	public static int getFreqOfItemInSample(int item, LinkedList<Data> leaveOneOutList){
		int count = 0;
		// check each transaction in sample
		for(Data data : leaveOneOutList){
			if(isItemExistInArr(data.getTran(),item)){
				count++;
			}
		}
		return count;
	}
	
	
	
	/** insert transaction into histogram **/
	public static void insertTranIntoHist(HashMap<Integer,Integer> hist, int[] tran){
		for(int i : tran){
			if(hist.get(i) != null)
				hist.put(i, hist.get(i) + 1); // if exist, ++
			else
				hist.put(i, 1); // if not exist, set 1
		}
	}
	
	/** remove transaction into histogram **/
	public static void removeTranIntoHist(HashMap<Integer,Integer> hist, int[] tran){
		for(int i : tran){
			if(hist.get(i) == 1){
				hist.remove(i); // it will disappear
			}else{
				hist.put(i, hist.get(i) - 1); // count decrase
			}
		}
	}
	
	/** whether item exist in array or not **/
	public static boolean isItemExistInArr(int[] tran, int item){
		for(int i : tran){
			if(i == item)
				return true; // found
		}
		return false; // not found
	}
	
	/** print contents in the sample **/
	public static void printSampleListInfo(LinkedList<Data> list){
		for(Data data : list){
//			System.out.println("tran : " + printIntArray(data.getTran()) +
			printIntArray(data.getTran());
			System.out.print(",  distance = "); // + data.getDistance());
			System.out.printf("%f\n", data.getDistance());
		}
		System.out.println("sample size = " + list.size());
		System.out.println();
	}
	
	/** print contents in the sample **/
	public static void printSampleListInfoHead(LinkedList<Data> list, int n){
		int i = 0;
		for(Data data : list){
			if(i >= 10)
				break;
			printIntArray(data.getTran());
			System.out.print(",  distance = "); // + data.getDistance());
			System.out.printf("%f\n", data.getDistance());
			i++;
		}
		System.out.println("sample size = " + list.size());
		System.out.println();
	}
	
	public static void printIntArray(int[] array) {
		//System.out.println("=====start print() ============");
		for (int i : array) {
			System.out.print(i + " ");
		}
		//System.out.println();
		//System.out.println("=====end print() ============");
	}
	
	public static int[] transferLineToArray(String line) {
		String[] lineSplited = line.split(" ");
		int[] tran = new int[lineSplited.length];

		for (int i = 0; i < lineSplited.length; i++) {
			tran[i] = Integer.parseInt(lineSplited[i]);
		}
		return tran;
	}
	
	static void printMapContent(Map<Integer,Integer> map){
		for(Integer i : map.keySet()){
			System.out.println("key : " + i + ", value : " + map.get(i));
		}
	}

}
