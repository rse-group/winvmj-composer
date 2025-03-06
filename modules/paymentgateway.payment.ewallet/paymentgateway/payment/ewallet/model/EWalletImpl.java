package paymentgateway.payment.ewallet;

import paymentgateway.payment.core.PaymentDecorator;

import java.util.HashMap;

import paymentgateway.payment.core.PaymentComponent;

import javax.persistence.Entity;
import javax.persistence.Table;
@Entity(name = "ewallet_impl")
@Table(name = "ewallet_impl")
public class EWalletImpl extends PaymentDecorator {

	protected String phoneNumber;
	protected String eWalletType;
	protected String eWalletUrl;
	public EWalletImpl(PaymentComponent record, String phoneNumber, String eWalletType, String eWalletUrl) {
		super(record);
		this.phoneNumber = phoneNumber;
		this.eWalletType = eWalletType;
		this.eWalletUrl = eWalletUrl;
	}

	public EWalletImpl(){
		super();
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEWalletType() {
		return this.eWalletType;
	}

	public void setEWalletType(String eWalletType) {
		this.eWalletType = eWalletType;
	}
	public String getEWalletUrl() {
		return this.eWalletUrl;
	}

	public void setEWalletUrl(String eWalletUrl) {
		this.eWalletUrl = eWalletUrl;
	}

	public HashMap<String,Object> toHashMap() {
		HashMap<String,Object> ewalletMap = record.toHashMap();
		ewalletMap.put("phoneNumber", getPhoneNumber());
		ewalletMap.put("eWalletType", getEWalletType());
		ewalletMap.put("eWalletUrl", getEWalletUrl());
		return ewalletMap;
	}
}
