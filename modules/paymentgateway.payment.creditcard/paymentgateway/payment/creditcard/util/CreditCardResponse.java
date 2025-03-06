package paymentgateway.payment.creditcard;

public class CreditCardResponse {

	private int id;
	private String status_code;
	private String status_message;
	private String transaction_id;
	private String order_id;
	private String redirect_url;
	private String gross_amount;
	private String currency;
	private String payment_type;
	private String transaction_time;
	private String transaction_status;
	private String fraud_status;
	private String masked_card;
	private String bank;
	private String card_type;
	private String three_ds_version;
	private boolean challenge_completion;

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
	public int getId() {

		return Integer.parseInt(order_id);
	}
	public void setId(int order_id) {
		this.id = order_id;
	}
	public String getRedirect_url() {
		return redirect_url;
	}
	public void setRedirect_url(String redirect_url) {
		this.redirect_url = redirect_url;
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
	public String getPayment_type() {
		return payment_type;
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
	public String getMasked_card() {
		return masked_card;
	}
	public void setMasked_card(String masked_card) {
		this.masked_card = masked_card;
	}
	public String getBank() {
		return bank;
	}
	public void setBank(String bank) {
		this.bank = bank;
	}
	public String getCard_type() {
		return card_type;
	}
	public void setCard_type(String card_type) {
		this.card_type = card_type;
	}
	public String getThree_ds_version() {
		return three_ds_version;
	}
	public void setThree_ds_version(String three_ds_version) {
		this.three_ds_version = three_ds_version;
	}
	public boolean isChallenge_completion() {
		return challenge_completion;
	}
	public void setChallenge_completion(boolean challenge_completion) {
		this.challenge_completion = challenge_completion;
	}
}
