import java.text.NumberFormat;

public class Test {
	public static void main(String[] args) {
		NumberFormat numberFormat = NumberFormat.getPercentInstance();
		numberFormat.setMinimumFractionDigits(0);
		System.out.println(numberFormat.format((float)2/100));
	}
}
