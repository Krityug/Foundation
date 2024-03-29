package org.mineacademy.fo;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for mathematical operations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MathUtil {

	/**
	 * Formatter that transforms whole numbers into whole decimals with 1 decimal point
	 */
	private final static DecimalFormat oneDigitFormat = new DecimalFormat("#.#");

	/**
	 * Formatter that transforms whole numbers into whole decimals with 2 decimal points
	 */
	private final static DecimalFormat twoDigitsFormat = new DecimalFormat("#.##");

	/**
	 * Formatter that transforms whole numbers into whole decimals with 3 decimal points
	 */
	private final static DecimalFormat threeDigitsFormat = new DecimalFormat("#.###");

	/**
	 * Formatter that transforms whole numbers into whole decimals with 5 decimal points
	 */
	private final static DecimalFormat fiveDigitsFormat = new DecimalFormat("#.#####");

	/**
	 * Holds all valid roman numbers
	 */
	private final static NavigableMap<Integer, String> romanNumbers = new TreeMap<>();

	// Load the roman numbers
	static {
		romanNumbers.put(1000, "M");
		romanNumbers.put(900, "CM");
		romanNumbers.put(500, "D");
		romanNumbers.put(400, "CD");
		romanNumbers.put(100, "C");
		romanNumbers.put(90, "XC");
		romanNumbers.put(50, "L");
		romanNumbers.put(40, "XL");
		romanNumbers.put(10, "X");
		romanNumbers.put(9, "IX");
		romanNumbers.put(5, "V");
		romanNumbers.put(4, "IV");
		romanNumbers.put(1, "I");
	}

	// ----------------------------------------------------------------------------------------------------
	// Number manipulation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Return a roman number representation of the given number
	 *
	 * @param number
	 * @return
	 */
	public static String toRoman(int number) {
		final int literal = romanNumbers.floorKey(number);

		if (number == literal)
			return romanNumbers.get(number);

		return romanNumbers.get(literal) + toRoman(number - literal);
	}

	/**
	 * See {@link Math#floor(double)}
	 *
	 * @param d1
	 * @return
	 */
	public static int floor(double d1) {
		final int i = (int) d1;

		return d1 >= i ? i : i - 1;
	}

	/**
	 * See {@link Math#ceil(double)}
	 *
	 * @param f1
	 * @return
	 */
	public static int ceiling(double f1) {
		final int i = (int) f1;

		return f1 >= i ? i : i - 1;
	}

	/**
	 * See {@link #range(int, int, int)}
	 *
	 * @param value the real value
	 * @param min the min limit
	 * @param max the max limit
	 * @return the value in range
	 */
	public static double range(double value, double min, double max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * Get a value in range. If the value is < min, returns min, if it is > max, returns max.
	 *
	 * @param value the real value
	 * @param min the min limit
	 * @param max the max limit
	 * @return the value in range
	 */
	public static int range(int value, int min, int max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * Return the given value if above min, or min
	 *
	 * @param value
	 * @param min
	 * @return
	 */
	public static double atLeast(double value, double min) {
		return value > min ? value : min;
	}

	/**
	 * Return the given value if above min, or min
	 *
	 * @param value
	 * @param min
	 * @return
	 */
	public static int atLeast(int value, int min) {
		return value > min ? value : min;
	}

	/**
	 * Increase the given number by given percents (from 0 to 100)
	 *
	 * @param number
	 * @param percent
	 * @return
	 */
	public static int increase(int number, double percent) {
		final double myNumber = number;
		final double percentage = myNumber / 100 * percent;

		return (int) Math.round(myNumber + percentage);
	}

	/**
	 * Increase the given number by given percents (from 0 to 100)
	 *
	 * @param number
	 * @param percent
	 * @return
	 */
	public static double increase(double number, double percent) {
		final double percentage = number / 100 * percent;

		return number + percentage;
	}

	/**
	 * Calculates the percentage (completion) of the given number from the maximum
	 * in 0 till 100
	 *
	 * @param number
	 * @param maximum
	 * @return 0 to 100 of the given number portion of the maximum
	 */
	public static int percent(double number, double maximum) {
		return (int) (number / maximum * 100);
	}

	/**
	 * Return the average double of the given values
	 *
	 * @param values
	 * @return
	 */
	public static double average(Collection<Double> values) {
		return average(values.toArray(new Double[values.size()]));
	}

	/**
	 * Return the average double of the given values
	 *
	 * @param values
	 * @return
	 */
	public static double average(Double... values) {
		double sum = 0;

		for (final double val : values)
			sum += val;

		return formatTwoDigitsD(sum / values.length);
	}

	// ----------------------------------------------------------------------------------------------------
	// Formatting
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Formats the given number into one digit
	 *
	 * @param value
	 * @return
	 */
	public static String formatOneDigit(double value) {
		return oneDigitFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into one digit
	 *
	 * @param value
	 * @return
	 */
	public static double formatOneDigitD(double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(oneDigitFormat.format(value).replace(",", "."));
	}

	/**
	 * Formats the given number into two digits
	 *
	 * @param value
	 * @return
	 */
	public static String formatTwoDigits(double value) {
		return twoDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into two digits
	 *
	 * @param value
	 * @return
	 */
	public static double formatTwoDigitsD(double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(twoDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * Formats the given number into three digits
	 *
	 * @param value
	 * @return
	 */
	public static String formatThreeDigits(double value) {
		return threeDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into three digits
	 *
	 * @param value
	 * @return
	 */
	public static double formatThreeDigitsD(double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(threeDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * Formats the given number into five digits
	 *
	 * @param value
	 * @return
	 */
	public static String formatFiveDigits(double value) {
		return fiveDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * Formats the given number into five digits
	 *
	 * @param value
	 * @return
	 */
	public static double formatFiveDigitsD(double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(fiveDigitsFormat.format(value).replace(",", "."));
	}

	// ----------------------------------------------------------------------------------------------------
	// Calculating
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Evaluate the given expression, e.g. 5*(4-2) returns... let me check!
	 *
	 * @param expression
	 * @return
	 */
	public static double calculate(final String expression) {
		class Parser {
			int pos = -1, c;

			void eatChar() {
				c = ++pos < expression.length() ? expression.charAt(pos) : -1;
			}

			void eatSpace() {
				while (Character.isWhitespace(c))
					eatChar();
			}

			double parse() {
				eatChar();

				final double v = parseExpression();

				if (c != -1)
					throw new CalculatorException("Unexpected: " + (char) c);

				return v;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor | term brackets
			// factor = brackets | number | factor `^` factor
			// brackets = `(` expression `)`

			double parseExpression() {
				double v = parseTerm();

				for (;;) {
					eatSpace();

					if (c == '+') { // addition
						eatChar();
						v += parseTerm();
					} else if (c == '-') { // subtraction
						eatChar();
						v -= parseTerm();
					} else
						return v;

				}
			}

			double parseTerm() {
				double v = parseFactor();

				for (;;) {
					eatSpace();

					if (c == '/') { // division
						eatChar();
						v /= parseFactor();
					} else if (c == '*' || c == '(') { // multiplication
						if (c == '*')
							eatChar();
						v *= parseFactor();
					} else
						return v;
				}
			}

			double parseFactor() {
				double v;
				boolean negate = false;

				eatSpace();

				if (c == '+' || c == '-') { // unary plus & minus
					negate = c == '-';
					eatChar();
					eatSpace();
				}

				if (c == '(') { // brackets
					eatChar();
					v = parseExpression();
					if (c == ')')
						eatChar();
				} else { // numbers
					final StringBuilder sb = new StringBuilder();

					while (c >= '0' && c <= '9' || c == '.') {
						sb.append((char) c);
						eatChar();
					}

					if (sb.length() == 0)
						throw new CalculatorException("Unexpected: " + (char) c);

					v = Double.parseDouble(sb.toString());
				}
				eatSpace();
				if (c == '^') { // exponentiation
					eatChar();
					v = Math.pow(v, parseFactor());
				}
				if (negate)
					v = -v; // unary minus is applied after exponentiation; e.g. -3^2=-9
				return v;
			}
		}
		return new Parser().parse();
	}

	/**
	 * An exception thrown when calculating wrong numbers (i.e. 0 division)
	 *
	 * See {@link MathUtil#calculate(String)}
	 */
	public static final class CalculatorException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public CalculatorException(String message) {
			super(message);
		}
	}
}