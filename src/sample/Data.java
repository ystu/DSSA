package sample;

public class Data {
	private int[] tran;
	private double distance = 0;
	
	public Data(int[] tran, double distance){
		this.tran = tran;
		this.distance = distance;
	}
	
	
	
	
	
	
	public int[] getTran() {
		return tran;
	}
	public void setTran(int[] tran) {
		this.tran = tran;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	
}
