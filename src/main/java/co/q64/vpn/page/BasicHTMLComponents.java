package co.q64.vpn.page;

public class BasicHTMLComponents {
	public static String BEGIN = "<html><body>";
	public static String END = "</body></html>";
	public static String BR = "<br>";
	public static String SCRIPT_BEGIN = "<script>";
	public static String SCRIPT_END = "</script>";
	public static String LINK_START = "<a href=\"";
	public static String LINK_END = "\">";
	public static String LINK_FINAL = "</a>";
	public static String FORM_BEGIN_ACTION = "<form action=\"";
	public static String FORM_END_ACTION = "\" method=\"post\">";
	public static String FORM_FINAL = "</form>";
	public static String FORM_TEXT_BEGIN = "<input type=\"text\" name=\"";
	public static String FORM_TEXT_END = "\">";
	public static String FORM_SUBMIT = "<button type=\"submit\">Submit</button>";

	public static String BEGIN_LOGIN = "<html><head><script type=\"text/javascript\" src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script><link rel=\"stylesheet\" type=\"text/css\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\"><link rel=\"stylesheet\" type=\"text/css\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css\"><link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.rawgit.com/lipis/bootstrap-social/gh-pages/bootstrap-social.css\"></head><body>";
	public static String GH_BUTTON_BEGIN = "<a class=\"btn btn-block btn-social btn-github\" href=\"";
	public static String GH_BUTTON_FINAL = "\"><span class=\"fa fa-github\"></span> Sign in with GitHub</a>";
	public static String GOOGLE_BUTTON_BEGIN = "<a class=\"btn btn-block btn-social btn-google\" href=\"";
	public static String GOOGLE_BUTTON_FINAL = "\"><span class=\"fa fa-google\"></span> Sign in with GitHub</a>";

	public static String PAYPAL_CHECKOUT = "<script src=\"https://www.paypalobjects.com/api/checkout.js\"></script>";
	public static String BEGIN_ACC = "<html ng-app><head><script type=\"text/javascript\" src=\"https://cdn.rawgit.com/mckamey/countdownjs/master/countdown.js\"></script><script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/angularjs/1.6.1/angular.js\"></script>" + PAYPAL_CHECKOUT + "</head><body>";
	public static String PAYPAL_BUTTON = ""

	+ "Select number of weeks to buy <select id=\"select\" ng-init=\"somethingHere = options[1]\" ng-model=\"selectedPlan\">"

	+ "<option value=\"\" selected=\"selected\">-- SELECT --</option>"

	+ "<option value=\"1\">1</option>"

	+ "<option value=\"2\">2</option>"

	+ "<option value=\"5\">5</option>"

	+ "</select><br>Total cost: ${{selectedPlan}}"

	+ "<div id=\"paypal-button\"></div><script>" + "paypal.Button.render({" +

	" env: 'production'," +

	"  payment: function(resolve, reject) {" +

	"    var CREATE_PAYMENT_URL = 'https://q64.co/paypalnew/go?time=' + document.getElementById(\"select\").options[document.getElementById(\"select\").selectedIndex].value;" +

	"  paypal.request.post(CREATE_PAYMENT_URL)" +

	"    .then(function(data) { resolve(data.paymentID); })" +

	"          .catch(function(err) { reject(err); });" + "  }," +

	"  onAuthorize: function(data) {" +

	"     var EXECUTE_PAYMENT_URL = 'https://q64.co/paypalexec/go?id=' + data.paymentID + '&payer=' + data.payerID;" +

	"    paypal.request.post(EXECUTE_PAYMENT_URL)" +

	"        .then(function(data) { window.location.href=\"paypaldone\"; })" +

	"        .catch(function(err) { window.location.href=\"paypalfail\"; });"

	+ "}" +

	"}, '#paypal-button');</script>";
}
