/*
 * Copyright 2015-2020 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.MockCsvAnnotationBuilder.csvSource;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * @since 5.0
 */
class CsvArgumentsProviderTests {

	@Test
	void throwsExceptionOnInvalidCsv() {
		CsvSource annotation = csvSource("foo", "bar", "");

		assertThatExceptionOfType(JUnitException.class)//
				.isThrownBy(() -> provideArguments(annotation).toArray())//
				.withMessage("Line at index 2 contains invalid CSV: \"\"");
	}

	@Test
	void providesSingleArgument() {
		CsvSource annotation = csvSource("foo");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo"));
	}

	@Test
	void providesMultipleArguments() {
		CsvSource annotation = csvSource("foo", "bar");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void splitsAndTrimsArguments() {
		CsvSource annotation = csvSource(" foo , bar ");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo", "bar"));
	}

	@Test
	void trimsLeadingSpaces() {
		CsvSource annotation = csvSource("'', 1", " '', 2", "'' , 3", " '' , 4");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(new Object[][] { { "", "1" }, { "", "2" }, { "", "3" }, { "", "4" } });
	}

	@Test
	void trimsTrailingSpaces() {
		CsvSource annotation = csvSource("1,''", "2, ''", "3,'' ", "4, '' ");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(new Object[][] { { "1", "" }, { "2", "" }, { "3", "" }, { "4", "" } });
	}

	@Test
	void understandsQuotes() {
		CsvSource annotation = csvSource("'foo, bar'");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo, bar"));
	}

	@Test
	void understandsEscapeCharacters() {
		CsvSource annotation = csvSource("'foo or ''bar''', baz");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo or 'bar'", "baz"));
	}

	@Test
	void providesArgumentsWithCharacterDelimiter() {
		CsvSource annotation = csvSource().delimiter('|').lines("foo|bar", "bar|foo").build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo", "bar"), array("bar", "foo"));
	}

	@Test
	void providesArgumentsWithStringDelimiter() {
		CsvSource annotation = csvSource().delimiterString("~~~").lines("foo~~~ bar", "bar~~~ foo").build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo", "bar"), array("bar", "foo"));
	}

	@Test
	void throwsExceptionIfBothDelimitersAreSimultaneouslySet() {
		CsvSource annotation = csvSource().delimiter('|').delimiterString("~~~").build();

		assertThatExceptionOfType(PreconditionViolationException.class)//
				.isThrownBy(() -> provideArguments(annotation))//
				.withMessageStartingWith("The delimiter and delimiterString attributes cannot be set simultaneously in")//
				.withMessageContaining("CsvSource");
	}

	@Test
	void defaultEmptyValueAndDefaultNullValue() {
		CsvSource annotation = csvSource("'', null, , apple");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("", "null", null, "apple"));
	}

	@Test
	void customEmptyValueAndDefaultNullValue() {
		CsvSource annotation = csvSource().emptyValue("EMPTY").lines("'', null, , apple").build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("EMPTY", "null", null, "apple"));
	}

	@Test
	void customNullValues() {
		CsvSource annotation = csvSource().nullValues("N/A", "NIL").lines("apple, , NIL, '', N/A, banana").build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("apple", null, null, "", null, "banana"));
	}

	@Test
	void convertsEmptyValuesToNullInLinesAfterFirstLine() {
		CsvSource annotation = csvSource("'', ''", " , ");

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(new Object[][] { { "", "" }, { null, null } });
	}

	@Test
	void throwsExceptionIfSourceExceedsMaxCharsPerColumnConfig() {
		CsvSource annotation = csvSource().lines("413").maxCharsPerColumn(2).build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThatExceptionOfType(CsvParsingException.class)//
				.isThrownBy(() -> arguments.toArray())//
				.withMessageStartingWith("Failed to parse CSV input configured via Mock for CsvSource")//
				.withRootCauseInstanceOf(ArrayIndexOutOfBoundsException.class);
	}

	@Test
	void providesArgumentWithDefaultMaxCharsPerColumnConfig() {
		CsvSource annotation = csvSource().lines( //4096 chars
			"41,6,8469,0,22,6336,9177,null,3,15,31,5734,6509,8770,9379,2,4,10,20,28,39,1490,5775,6410,6986,8493,8823,"
					+ "9298,9975,1,null,null,5,8,12,16,21,26,29,34,40,506,4491,5737,6034,6384,6434,6853,8178,8492,"
					+ "8503,8779,9106,9283,9339,9774,9982,null,null,null,null,7,9,11,13,null,17,null,null,23,27,null,30,"
					+ "32,35,null,null,176,837,3317,5715,5735,5743,5944,6310,6338,6398,6425,6436,6802,6917,7473,8397,"
					+ "8472,null,8494,8711,8776,8782,9009,9153,9210,9289,9336,9371,9400,9953,9980,9993,null,null,null,"
					+ "null,null,null,null,14,null,18,null,25,null,null,null,null,null,33,null,36,53,316,739,1409,3011,"
					+ "3924,4845,5730,null,5736,5741,5754,5807,5968,6056,6329,6337,6345,6387,6400,6420,6428,6435,6467,"
					+ "6575,6814,6911,6970,7111,7761,8283,8461,8470,8473,null,8496,8674,8722,8773,8778,8780,8803,8924,"
					+ "9078,9111,9172,9185,9270,9285,9296,9307,9337,9369,9376,9391,9633,9933,9967,9979,9981,9989,9994,"
					+ "null,null,null,19,24,null,null,null,null,37,46,140,199,505,669,814,1281,1430,1976,3096,3627,3934,"
					+ "4646,5455,5719,5733,null,null,5740,5742,5752,5758,5790,5828,5948,5974,6051,6278,6326,6331,null,"
					+ "null,6339,6365,6385,6388,6399,6405,6417,6424,6426,6433,null,null,6441,6492,6572,6739,6808,6845,"
					+ "6867,6915,6931,6985,7086,7463,7758,7797,8179,8349,8421,8467,null,8471,null,8476,8495,8497,8576,"
					+ "8708,8719,8761,8771,8774,8777,null,null,8781,8793,8806,8882,8937,9054,9084,9107,9134,9166,9175,"
					+ "9181,9187,9255,9282,9284,9287,9292,9297,9305,9319,null,9338,9342,9370,9375,9378,9380,9396,9546,"
					+ "9713,9798,9946,9956,9973,9978,null,null,null,9986,9991,null,9999,null,null,null,null,null,38,45,"
					+ "49,111,175,196,203,352,null,631,673,799,832,1056,1366,1415,1480,1819,2471,3016,3223,3532,3770,"
					+ "3931,4018,4592,4835,5233,5477,5718,5728,5732,null,5739,null,null,null,5745,5753,5756,5766,5785,"
					+ "5803,5823,5875,5946,5950,5971,5994,6043,6052,6124,6298,6317,6328,6330,6332,null,6341,6348,6369,"
					+ "null,6386,null,6396,null,null,6404,6408,6415,6419,6422,null,null,6427,6430,null,6440,6458,6471,"
					+ "6503,6520,6574,6721,6750,6807,6810,6843,6849,6863,6893,6912,6916,6919,6947,6983,null,6993,7088,"
					+ "7124,7466,7703,7759,7787,7962,null,8195,8331,8362,8411,8453,8462,8468,null,null,8474,8482,null,"
					+ "null,null,8500,8573,8619,8682,8710,8712,8721,8755,8767,null,8772,null,8775,null,null,null,null,"
					+ "8787,8795,8805,8819,8862,8886,8929,8978,9014,9076,9080,9087,null,9110,9112,9139,9163,9170,9173,"
					+ "9176,9178,9183,9186,9188,9240,9267,9278,null,null,null,9286,9288,9290,9293,null,null,9303,9306,"
					+ "9312,9326,null,null,9340,9366,null,null,9374,null,9377,null,null,9383,9393,9397,9493,9601,9652,"
					+ "9726,9789,9905,9942,9947,9954,9959,9969,9974,9976,null,9984,9988,9990,9992,9997,null,null,null,"
					+ "44,null,48,52,92,125,156,null,183,197,202,235,325,366,629,656,670,712,746,802,816,835,1026,1212,"
					+ "1293,1403,1414,1420,1471,1483,1604,1903,2432,2870,3014,3062,3219,3311,3336,3565,3730,3918,3927,"
					+ "3933,3975,4330,4546,4627,4709,4840,5158,5399,5456,5697,5716,null,5727,5729,5731,null,5738,null,"
					+ "5744,5746,null,null,5755,5757,5762,5774,5778,5786,5795,5805,5822,5824,5848,5895,5945,5947,5949,"
					+ "5961,5969,5973,5976,6014,6042,6049,null,6053,6117,6139,6290,6308,6314,6320,6327,null,null,null,"
					+ "null,6335,6340,6344,6346,6354,6366,6383,null,null,6390,6397,6401,null,6406,6409,6413,6416,6418,"
					+ "null,6421,6423,null,null,6429,6431,6438,null,6443,6462,6470,6472,6497,6506,6512,6528,6573,null,"
					+ "6648,6733,6748,6767,6805,null,6809,6813,6828,6844,6847,6852,6854,6864,6875,6910,null,6914,null,"
					+ "null,6918,6922,6944,6968,6976,6984,6991,7047,7087,7091,7112,7260,7464,7467,7656,7740,null,7760,"
					+ "7774,7795,7876,8110,8187,8202,8315,8341,8359,8391,8410,8414,8448,8455,null,8463,null,null,null,"
					+ "8475,8478,8483,8498,8502,8531,8575,8600,8624,8681,8688,8709,null,null,8716,8720,null,8735,8760,"
					+ "8766,8768,null,null,null,null,8784,8791,8794,8797,8804,null,8815,8820,8825,8867,8884,8887,8927,"
					+ "8936,8974,8986,9013,9041,9067,9077,9079,9081,9086,9093,9109,null,null,9119,9138,9144,9159,9164,"
					+ "9167,9171,null,9174,null,null,null,9179,9182,9184,null,null,null,9189,9223,9247,9258,9269,9275,"
					+ "9279,null,null,null,null,null,9291,null,9295,9301,9304,null,null,9310,9315,9324,9330,null,9341,"
					+ "9358,9367,9372,null,null,null,9382,9386,9392,9395,null,9398,9488,9509,9547,9616,9643,9702,94,122").delimiter(
						';').build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments.toArray()).hasSize(1);
	}

	@Test
	void throwsExceptionWhenSourceExceedsDefaultMaxCharsPerColumnConfig() {
		CsvSource annotation = csvSource().lines( //4097 chars
			"41,6,8469,0,22,6336,9177,null,3,15,31,5734,6509,8770,9379,2,4,10,20,28,39,1490,5775,6410,6986,8493,8823,"
					+ "9298,9975,1,null,null,5,8,12,16,21,26,29,34,40,506,4491,5737,6034,6384,6434,6853,8178,8492,"
					+ "8503,8779,9106,9283,9339,9774,9982,null,null,null,null,7,9,11,13,null,17,null,null,23,27,null,30,"
					+ "32,35,null,null,176,837,3317,5715,5735,5743,5944,6310,6338,6398,6425,6436,6802,6917,7473,8397,"
					+ "8472,null,8494,8711,8776,8782,9009,9153,9210,9289,9336,9371,9400,9953,9980,9993,null,null,null,"
					+ "null,null,null,null,14,null,18,null,25,null,null,null,null,null,33,null,36,53,316,739,1409,3011,"
					+ "3924,4845,5730,null,5736,5741,5754,5807,5968,6056,6329,6337,6345,6387,6400,6420,6428,6435,6467,"
					+ "6575,6814,6911,6970,7111,7761,8283,8461,8470,8473,null,8496,8674,8722,8773,8778,8780,8803,8924,"
					+ "9078,9111,9172,9185,9270,9285,9296,9307,9337,9369,9376,9391,9633,9933,9967,9979,9981,9989,9994,"
					+ "null,null,null,19,24,null,null,null,null,37,46,140,199,505,669,814,1281,1430,1976,3096,3627,3934,"
					+ "4646,5455,5719,5733,null,null,5740,5742,5752,5758,5790,5828,5948,5974,6051,6278,6326,6331,null,"
					+ "null,6339,6365,6385,6388,6399,6405,6417,6424,6426,6433,null,null,6441,6492,6572,6739,6808,6845,"
					+ "6867,6915,6931,6985,7086,7463,7758,7797,8179,8349,8421,8467,null,8471,null,8476,8495,8497,8576,"
					+ "8708,8719,8761,8771,8774,8777,null,null,8781,8793,8806,8882,8937,9054,9084,9107,9134,9166,9175,"
					+ "9181,9187,9255,9282,9284,9287,9292,9297,9305,9319,null,9338,9342,9370,9375,9378,9380,9396,9546,"
					+ "9713,9798,9946,9956,9973,9978,null,null,null,9986,9991,null,9999,null,null,null,null,null,38,45,"
					+ "49,111,175,196,203,352,null,631,673,799,832,1056,1366,1415,1480,1819,2471,3016,3223,3532,3770,"
					+ "3931,4018,4592,4835,5233,5477,5718,5728,5732,null,5739,null,null,null,5745,5753,5756,5766,5785,"
					+ "5803,5823,5875,5946,5950,5971,5994,6043,6052,6124,6298,6317,6328,6330,6332,null,6341,6348,6369,"
					+ "null,6386,null,6396,null,null,6404,6408,6415,6419,6422,null,null,6427,6430,null,6440,6458,6471,"
					+ "6503,6520,6574,6721,6750,6807,6810,6843,6849,6863,6893,6912,6916,6919,6947,6983,null,6993,7088,"
					+ "7124,7466,7703,7759,7787,7962,null,8195,8331,8362,8411,8453,8462,8468,null,null,8474,8482,null,"
					+ "null,null,8500,8573,8619,8682,8710,8712,8721,8755,8767,null,8772,null,8775,null,null,null,null,"
					+ "8787,8795,8805,8819,8862,8886,8929,8978,9014,9076,9080,9087,null,9110,9112,9139,9163,9170,9173,"
					+ "9176,9178,9183,9186,9188,9240,9267,9278,null,null,null,9286,9288,9290,9293,null,null,9303,9306,"
					+ "9312,9326,null,null,9340,9366,null,null,9374,null,9377,null,null,9383,9393,9397,9493,9601,9652,"
					+ "9726,9789,9905,9942,9947,9954,9959,9969,9974,9976,null,9984,9988,9990,9992,9997,null,null,null,"
					+ "44,null,48,52,92,125,156,null,183,197,202,235,325,366,629,656,670,712,746,802,816,835,1026,1212,"
					+ "1293,1403,1414,1420,1471,1483,1604,1903,2432,2870,3014,3062,3219,3311,3336,3565,3730,3918,3927,"
					+ "3933,3975,4330,4546,4627,4709,4840,5158,5399,5456,5697,5716,null,5727,5729,5731,null,5738,null,"
					+ "5744,5746,null,null,5755,5757,5762,5774,5778,5786,5795,5805,5822,5824,5848,5895,5945,5947,5949,"
					+ "5961,5969,5973,5976,6014,6042,6049,null,6053,6117,6139,6290,6308,6314,6320,6327,null,null,null,"
					+ "null,6335,6340,6344,6346,6354,6366,6383,null,null,6390,6397,6401,null,6406,6409,6413,6416,6418,"
					+ "null,6421,6423,null,null,6429,6431,6438,null,6443,6462,6470,6472,6497,6506,6512,6528,6573,null,"
					+ "6648,6733,6748,6767,6805,null,6809,6813,6828,6844,6847,6852,6854,6864,6875,6910,null,6914,null,"
					+ "null,6918,6922,6944,6968,6976,6984,6991,7047,7087,7091,7112,7260,7464,7467,7656,7740,null,7760,"
					+ "7774,7795,7876,8110,8187,8202,8315,8341,8359,8391,8410,8414,8448,8455,null,8463,null,null,null,"
					+ "8475,8478,8483,8498,8502,8531,8575,8600,8624,8681,8688,8709,null,null,8716,8720,null,8735,8760,"
					+ "8766,8768,null,null,null,null,8784,8791,8794,8797,8804,null,8815,8820,8825,8867,8884,8887,8927,"
					+ "8936,8974,8986,9013,9041,9067,9077,9079,9081,9086,9093,9109,null,null,9119,9138,9144,9159,9164,"
					+ "9167,9171,null,9174,null,null,null,9179,9182,9184,null,null,null,9189,9223,9247,9258,9269,9275,"
					+ "9279,null,null,null,null,null,9291,null,9295,9301,9304,null,null,9310,9315,9324,9330,null,9341,"
					+ "9358,9367,9372,null,null,null,9382,9386,9392,9395,null,9398,9488,9509,9547,9616,9643,9702,94,1223").delimiter(
						';').build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThatExceptionOfType(CsvParsingException.class)//
				.isThrownBy(() -> arguments.toArray())//
				.withMessageStartingWith("Failed to parse CSV input configured via Mock for CsvSource")//
				.withRootCauseInstanceOf(ArrayIndexOutOfBoundsException.class);
	}

	@Test
	void providesArgumentsForExceedsSourceWithCustomMaxCharsPerColumnConfig() {
		CsvSource annotation = csvSource().lines( //4097 chars
			"41,6,8469,0,22,6336,9177,null,3,15,31,5734,6509,8770,9379,2,4,10,20,28,39,1490,5775,6410,6986,8493,8823,"
					+ "9298,9975,1,null,null,5,8,12,16,21,26,29,34,40,506,4491,5737,6034,6384,6434,6853,8178,8492,"
					+ "8503,8779,9106,9283,9339,9774,9982,null,null,null,null,7,9,11,13,null,17,null,null,23,27,null,30,"
					+ "32,35,null,null,176,837,3317,5715,5735,5743,5944,6310,6338,6398,6425,6436,6802,6917,7473,8397,"
					+ "8472,null,8494,8711,8776,8782,9009,9153,9210,9289,9336,9371,9400,9953,9980,9993,null,null,null,"
					+ "null,null,null,null,14,null,18,null,25,null,null,null,null,null,33,null,36,53,316,739,1409,3011,"
					+ "3924,4845,5730,null,5736,5741,5754,5807,5968,6056,6329,6337,6345,6387,6400,6420,6428,6435,6467,"
					+ "6575,6814,6911,6970,7111,7761,8283,8461,8470,8473,null,8496,8674,8722,8773,8778,8780,8803,8924,"
					+ "9078,9111,9172,9185,9270,9285,9296,9307,9337,9369,9376,9391,9633,9933,9967,9979,9981,9989,9994,"
					+ "null,null,null,19,24,null,null,null,null,37,46,140,199,505,669,814,1281,1430,1976,3096,3627,3934,"
					+ "4646,5455,5719,5733,null,null,5740,5742,5752,5758,5790,5828,5948,5974,6051,6278,6326,6331,null,"
					+ "null,6339,6365,6385,6388,6399,6405,6417,6424,6426,6433,null,null,6441,6492,6572,6739,6808,6845,"
					+ "6867,6915,6931,6985,7086,7463,7758,7797,8179,8349,8421,8467,null,8471,null,8476,8495,8497,8576,"
					+ "8708,8719,8761,8771,8774,8777,null,null,8781,8793,8806,8882,8937,9054,9084,9107,9134,9166,9175,"
					+ "9181,9187,9255,9282,9284,9287,9292,9297,9305,9319,null,9338,9342,9370,9375,9378,9380,9396,9546,"
					+ "9713,9798,9946,9956,9973,9978,null,null,null,9986,9991,null,9999,null,null,null,null,null,38,45,"
					+ "49,111,175,196,203,352,null,631,673,799,832,1056,1366,1415,1480,1819,2471,3016,3223,3532,3770,"
					+ "3931,4018,4592,4835,5233,5477,5718,5728,5732,null,5739,null,null,null,5745,5753,5756,5766,5785,"
					+ "5803,5823,5875,5946,5950,5971,5994,6043,6052,6124,6298,6317,6328,6330,6332,null,6341,6348,6369,"
					+ "null,6386,null,6396,null,null,6404,6408,6415,6419,6422,null,null,6427,6430,null,6440,6458,6471,"
					+ "6503,6520,6574,6721,6750,6807,6810,6843,6849,6863,6893,6912,6916,6919,6947,6983,null,6993,7088,"
					+ "7124,7466,7703,7759,7787,7962,null,8195,8331,8362,8411,8453,8462,8468,null,null,8474,8482,null,"
					+ "null,null,8500,8573,8619,8682,8710,8712,8721,8755,8767,null,8772,null,8775,null,null,null,null,"
					+ "8787,8795,8805,8819,8862,8886,8929,8978,9014,9076,9080,9087,null,9110,9112,9139,9163,9170,9173,"
					+ "9176,9178,9183,9186,9188,9240,9267,9278,null,null,null,9286,9288,9290,9293,null,null,9303,9306,"
					+ "9312,9326,null,null,9340,9366,null,null,9374,null,9377,null,null,9383,9393,9397,9493,9601,9652,"
					+ "9726,9789,9905,9942,9947,9954,9959,9969,9974,9976,null,9984,9988,9990,9992,9997,null,null,null,"
					+ "44,null,48,52,92,125,156,null,183,197,202,235,325,366,629,656,670,712,746,802,816,835,1026,1212,"
					+ "1293,1403,1414,1420,1471,1483,1604,1903,2432,2870,3014,3062,3219,3311,3336,3565,3730,3918,3927,"
					+ "3933,3975,4330,4546,4627,4709,4840,5158,5399,5456,5697,5716,null,5727,5729,5731,null,5738,null,"
					+ "5744,5746,null,null,5755,5757,5762,5774,5778,5786,5795,5805,5822,5824,5848,5895,5945,5947,5949,"
					+ "5961,5969,5973,5976,6014,6042,6049,null,6053,6117,6139,6290,6308,6314,6320,6327,null,null,null,"
					+ "null,6335,6340,6344,6346,6354,6366,6383,null,null,6390,6397,6401,null,6406,6409,6413,6416,6418,"
					+ "null,6421,6423,null,null,6429,6431,6438,null,6443,6462,6470,6472,6497,6506,6512,6528,6573,null,"
					+ "6648,6733,6748,6767,6805,null,6809,6813,6828,6844,6847,6852,6854,6864,6875,6910,null,6914,null,"
					+ "null,6918,6922,6944,6968,6976,6984,6991,7047,7087,7091,7112,7260,7464,7467,7656,7740,null,7760,"
					+ "7774,7795,7876,8110,8187,8202,8315,8341,8359,8391,8410,8414,8448,8455,null,8463,null,null,null,"
					+ "8475,8478,8483,8498,8502,8531,8575,8600,8624,8681,8688,8709,null,null,8716,8720,null,8735,8760,"
					+ "8766,8768,null,null,null,null,8784,8791,8794,8797,8804,null,8815,8820,8825,8867,8884,8887,8927,"
					+ "8936,8974,8986,9013,9041,9067,9077,9079,9081,9086,9093,9109,null,null,9119,9138,9144,9159,9164,"
					+ "9167,9171,null,9174,null,null,null,9179,9182,9184,null,null,null,9189,9223,9247,9258,9269,9275,"
					+ "9279,null,null,null,null,null,9291,null,9295,9301,9304,null,null,9310,9315,9324,9330,null,9341,"
					+ "9358,9367,9372,null,null,null,9382,9386,9392,9395,null,9398,9488,9509,9547,9616,9643,9702,94,1223").delimiter(
						';').maxCharsPerColumn(4097).build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments.toArray()).hasSize(1);
	}

	@Test
	void providesArgumentsForExceedsSourceWithAutoExpansionConfig() {
		CsvSource annotation = csvSource().lines( //4097 chars
			"41,6,8469,0,22,6336,9177,null,3,15,31,5734,6509,8770,9379,2,4,10,20,28,39,1490,5775,6410,6986,8493,8823,"
					+ "9298,9975,1,null,null,5,8,12,16,21,26,29,34,40,506,4491,5737,6034,6384,6434,6853,8178,8492,"
					+ "8503,8779,9106,9283,9339,9774,9982,null,null,null,null,7,9,11,13,null,17,null,null,23,27,null,30,"
					+ "32,35,null,null,176,837,3317,5715,5735,5743,5944,6310,6338,6398,6425,6436,6802,6917,7473,8397,"
					+ "8472,null,8494,8711,8776,8782,9009,9153,9210,9289,9336,9371,9400,9953,9980,9993,null,null,null,"
					+ "null,null,null,null,14,null,18,null,25,null,null,null,null,null,33,null,36,53,316,739,1409,3011,"
					+ "3924,4845,5730,null,5736,5741,5754,5807,5968,6056,6329,6337,6345,6387,6400,6420,6428,6435,6467,"
					+ "6575,6814,6911,6970,7111,7761,8283,8461,8470,8473,null,8496,8674,8722,8773,8778,8780,8803,8924,"
					+ "9078,9111,9172,9185,9270,9285,9296,9307,9337,9369,9376,9391,9633,9933,9967,9979,9981,9989,9994,"
					+ "null,null,null,19,24,null,null,null,null,37,46,140,199,505,669,814,1281,1430,1976,3096,3627,3934,"
					+ "4646,5455,5719,5733,null,null,5740,5742,5752,5758,5790,5828,5948,5974,6051,6278,6326,6331,null,"
					+ "null,6339,6365,6385,6388,6399,6405,6417,6424,6426,6433,null,null,6441,6492,6572,6739,6808,6845,"
					+ "6867,6915,6931,6985,7086,7463,7758,7797,8179,8349,8421,8467,null,8471,null,8476,8495,8497,8576,"
					+ "8708,8719,8761,8771,8774,8777,null,null,8781,8793,8806,8882,8937,9054,9084,9107,9134,9166,9175,"
					+ "9181,9187,9255,9282,9284,9287,9292,9297,9305,9319,null,9338,9342,9370,9375,9378,9380,9396,9546,"
					+ "9713,9798,9946,9956,9973,9978,null,null,null,9986,9991,null,9999,null,null,null,null,null,38,45,"
					+ "49,111,175,196,203,352,null,631,673,799,832,1056,1366,1415,1480,1819,2471,3016,3223,3532,3770,"
					+ "3931,4018,4592,4835,5233,5477,5718,5728,5732,null,5739,null,null,null,5745,5753,5756,5766,5785,"
					+ "5803,5823,5875,5946,5950,5971,5994,6043,6052,6124,6298,6317,6328,6330,6332,null,6341,6348,6369,"
					+ "null,6386,null,6396,null,null,6404,6408,6415,6419,6422,null,null,6427,6430,null,6440,6458,6471,"
					+ "6503,6520,6574,6721,6750,6807,6810,6843,6849,6863,6893,6912,6916,6919,6947,6983,null,6993,7088,"
					+ "7124,7466,7703,7759,7787,7962,null,8195,8331,8362,8411,8453,8462,8468,null,null,8474,8482,null,"
					+ "null,null,8500,8573,8619,8682,8710,8712,8721,8755,8767,null,8772,null,8775,null,null,null,null,"
					+ "8787,8795,8805,8819,8862,8886,8929,8978,9014,9076,9080,9087,null,9110,9112,9139,9163,9170,9173,"
					+ "9176,9178,9183,9186,9188,9240,9267,9278,null,null,null,9286,9288,9290,9293,null,null,9303,9306,"
					+ "9312,9326,null,null,9340,9366,null,null,9374,null,9377,null,null,9383,9393,9397,9493,9601,9652,"
					+ "9726,9789,9905,9942,9947,9954,9959,9969,9974,9976,null,9984,9988,9990,9992,9997,null,null,null,"
					+ "44,null,48,52,92,125,156,null,183,197,202,235,325,366,629,656,670,712,746,802,816,835,1026,1212,"
					+ "1293,1403,1414,1420,1471,1483,1604,1903,2432,2870,3014,3062,3219,3311,3336,3565,3730,3918,3927,"
					+ "3933,3975,4330,4546,4627,4709,4840,5158,5399,5456,5697,5716,null,5727,5729,5731,null,5738,null,"
					+ "5744,5746,null,null,5755,5757,5762,5774,5778,5786,5795,5805,5822,5824,5848,5895,5945,5947,5949,"
					+ "5961,5969,5973,5976,6014,6042,6049,null,6053,6117,6139,6290,6308,6314,6320,6327,null,null,null,"
					+ "null,6335,6340,6344,6346,6354,6366,6383,null,null,6390,6397,6401,null,6406,6409,6413,6416,6418,"
					+ "null,6421,6423,null,null,6429,6431,6438,null,6443,6462,6470,6472,6497,6506,6512,6528,6573,null,"
					+ "6648,6733,6748,6767,6805,null,6809,6813,6828,6844,6847,6852,6854,6864,6875,6910,null,6914,null,"
					+ "null,6918,6922,6944,6968,6976,6984,6991,7047,7087,7091,7112,7260,7464,7467,7656,7740,null,7760,"
					+ "7774,7795,7876,8110,8187,8202,8315,8341,8359,8391,8410,8414,8448,8455,null,8463,null,null,null,"
					+ "8475,8478,8483,8498,8502,8531,8575,8600,8624,8681,8688,8709,null,null,8716,8720,null,8735,8760,"
					+ "8766,8768,null,null,null,null,8784,8791,8794,8797,8804,null,8815,8820,8825,8867,8884,8887,8927,"
					+ "8936,8974,8986,9013,9041,9067,9077,9079,9081,9086,9093,9109,null,null,9119,9138,9144,9159,9164,"
					+ "9167,9171,null,9174,null,null,null,9179,9182,9184,null,null,null,9189,9223,9247,9258,9269,9275,"
					+ "9279,null,null,null,null,null,9291,null,9295,9301,9304,null,null,9310,9315,9324,9330,null,9341,"
					+ "9358,9367,9372,null,null,null,9382,9386,9392,9395,null,9398,9488,9509,9547,9616,9643,9702,94,1223").delimiter(
						';').maxCharsPerColumn(-1).build();

		Stream<Object[]> arguments = provideArguments(annotation);

		assertThat(arguments.toArray()).hasSize(1);
	}

	private Stream<Object[]> provideArguments(CsvSource annotation) {
		CsvArgumentsProvider provider = new CsvArgumentsProvider();
		provider.accept(annotation);
		return provider.provideArguments(null).map(Arguments::get);
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] array(T... elements) {
		return elements;
	}

}
