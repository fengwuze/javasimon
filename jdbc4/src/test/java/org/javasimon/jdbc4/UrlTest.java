package org.javasimon.jdbc4;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

/**
 * Tests for JDBC real URL detection.
 *
 * @author Radovan Sninsky
 * @since 2.4
 */
public class UrlTest {

	@DataProvider(name = "dp1")
	public Object[][] createTestData() {
		return new Object[][] {
			{"jdbc:simon:sqlserver://test;databaseName=testdb", "jdbc:sqlserver://test;databaseName=testdb"},
			// TODO: I'm not sure if I want to support this in JDBC4 in the future or how to properly configure this all
//			{"jdbc:simon:sqlserver://test;databaseName=testdb;simon_format=human;simon_console=t",
//					"jdbc:sqlserver://test;databaseName=testdb"},
//			{"jdbc:simon:h2:tcp://localhost:6762/testdb;simon_console=y", "jdbc:h2:tcp://localhost:6762/testdb"}
		};
	}

	@Test(dataProvider = "dp1")
	public void urlRealUrlTest(String url, String realUrl) {
		Assert.assertEquals(new Driver.Url(url).getRealUrl(), realUrl);
	}

	@DataProvider(name = "dp2")
	public Object[][] createTestDataForUrlDriverTest() {
		return new Object[][] {
			{"jdbc:simon:sqlserver://test;databaseName=testdb", "sqlserver"},
			{"jdbc:simon:mysql://localhost/someDb?useUnicode=yes&characterEncoding=UTF-8", "mysql"}
		};
	}

	@Test(dataProvider = "dp2")
	public void urlDriverTest(String url, String driver) {
		Assert.assertEquals(new Driver.Url(url).getDriverId(), driver);
	}
}
