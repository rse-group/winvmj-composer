package paymentgateway.payment.creditcard;

import paymentgateway.payment.core.PaymentDecorator;

import java.util.HashMap;

import paymentgateway.payment.core.Payment;
import paymentgateway.payment.core.PaymentComponent;

import javax.persistence.Entity;
import javax.persistence.Table;
@Entity(name = "creditcard_impl")
@Table(name = "creditcard_impl")
public class PaymentImpl extends PaymentDecorator {

	protected String idToken;
	protected String statusCreditPayment;
	public PaymentImpl(PaymentComponent record, String idToken, String statusCreditPayment) {
		super(record);
		this.idToken = idToken;
		this.statusCreditPayment = statusCreditPayment;
	}

	public PaymentImpl(){
		super();
	}

	public String getIdToken() {
		return this.idToken;
	}

	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}
	public String getstatusCreditPayment() {
		return this.statusCreditPayment;
	}

	public void setstatusCreditPayment(String statusCreditPayment) {
		this.statusCreditPayment = statusCreditPayment;
	}

	public HashMap<String,Object> toHashMap() {
		HashMap<String,Object> creditCardMap = record.toHashMap();
		creditCardMap.put("idToken", getIdToken());
		creditCardMap.put("statusCreditPayment", getstatusCreditPayment());
		return creditCardMap;
	}
}
