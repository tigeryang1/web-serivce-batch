package com.cn2.model;
/**
 * @author hyang
 *
 */
public class Device {
	
	private String id;
	private String cameraAccess;
	private String pushNotifications;
	private String type;	
	private String locationServices;
	private String updated;
	private String date;
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCameraAccess() {
		return cameraAccess;
	}
	public void setCameraAccess(String cameraAccess) {
		this.cameraAccess = cameraAccess;
	}
	public String getPushNotifications() {
		return pushNotifications;
	}
	public void setPushNotifications(String pushNotifications) {
		this.pushNotifications = pushNotifications;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getLocationServices() {
		return locationServices;
	}
	public void setLocationServices(String locationServices) {
		this.locationServices = locationServices;
	}
	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}



}
