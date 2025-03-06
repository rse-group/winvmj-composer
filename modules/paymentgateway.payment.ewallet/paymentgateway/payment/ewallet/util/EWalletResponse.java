package paymentgateway.payment.ewallet;

import java.util.*;

public class EWalletResponse {

	private int id;
	private String status_code;
	private String status_message;
	private String transaction_id;
	private String order_id;
	private String gross_amount;
	private String currency;
	private String payment_type;
	private String transaction_time;
	private String transaction_status;
	private String fraud_status;
	private List<EWalletAction> actions;

	// OY

	private String ewallet_trx_status;
	private String trx_id;
	private String ewallet_code;
	private String ewallet_url;
	private String amount;
	private String partner_trx_id;
	private String customer_id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getStatus(){
		if (status_code == null){
			return ewallet_trx_status;
		}
		return status_code;
	}

	public void setEwallet_trx_status(String ewallet_trx_status) {
		this.ewallet_trx_status = ewallet_trx_status;
	}

	public String getTrx_id() {
		return trx_id;
	}

	public void setTrx_id(String trx_id) {
		this.trx_id = trx_id;
	}

	public String getAmount() {
		if (gross_amount == null){
			return amount;
		}
		return gross_amount;
	}

	public String getCustomer_id() {
		return customer_id;
	}

	public String getPayment_type() {
		if (payment_type == null){
			return ewallet_code;
		}
		return payment_type;
	}

	public String getUrl() {
		if (ewallet_url == null){
			return actions.get(0).getUrl();
		}
		return ewallet_url;
	}

	public String getPartner_trx_id() {
		return partner_trx_id;
	}

	public void setPartner_trx_id(String partner_trx_id) {
		this.partner_trx_id = partner_trx_id;
	}

	public String getStatus_code() {
		return status_code;
	}

	public void setStatus_code(String status_code) {
		this.status_code = status_code;
	}

	public String getStatus_message() {
		return status_message;
	}

	public void setStatus_message(String status_message) {
		this.status_message = status_message;
	}

	public String getTransaction_id() {
		return transaction_id;
	}

	public void setTransaction_id(String transaction_id) {
		this.transaction_id = transaction_id;
	}

	public String getOrder_id() {
		return order_id;
	}

	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}

	public String getGross_amount() {
		return gross_amount;
	}

	public void setGross_amount(String gross_amount) {
		this.gross_amount = gross_amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}


	public void setPayment_type(String payment_type) {
		this.payment_type = payment_type;
	}

	public String getTransaction_time() {
		return transaction_time;
	}

	public void setTransaction_time(String transaction_time) {
		this.transaction_time = transaction_time;
	}

	public String getTransaction_status() {
		return transaction_status;
	}

	public void setTransaction_status(String transaction_status) {
		this.transaction_status = transaction_status;
	}

	public String getFraud_status() {
		return fraud_status;
	}

	public void setFraud_status(String fraud_status) {
		this.fraud_status = fraud_status;
	}

	public List<EWalletAction> getActions() {
		return actions;
	}

	public void setActions(List<EWalletAction> actions) {
		this.actions = actions;
	}
}