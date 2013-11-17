package com.widjaja.hendra.geolocation;

public class MyPosition {
	private int id;
	private String latitude;
	private String longitude;
	private String remark;
	private String reserve;
	
	public MyPosition(){}
	
	public MyPosition(String latitude, String longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public String getReserve() {
		return reserve;
	}
	public void setReserve(String reserve) {
		this.reserve = reserve;
	}
	
	@Override
	public String toString() {
		return "Location [id=" + id + ", saved latitude: " + latitude + ", saved longitude: " + longitude + "]";
	}
}
