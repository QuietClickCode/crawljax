package com.crawljax.browser;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.google.common.base.Strings;

public class BrowserProvider extends ExternalResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(BrowserProvider.class);
	private List<RemoteWebDriver> usedBrowsers;

	public static EmbeddedBrowser.BrowserType getBrowserType() {
		String browser = System.getProperty("test.browser");
		if (!Strings.isNullOrEmpty(browser)) {
			return EmbeddedBrowser.BrowserType.valueOf(browser);
		} else {
			return EmbeddedBrowser.BrowserType.FIREFOX;
		}
	}

	@Override
	protected void before() throws Throwable {
		usedBrowsers = new LinkedList<>();
	}

	public EmbeddedBrowser newEmbeddedBrowser() {
		return WebDriverBackedEmbeddedBrowser.withDriver(newBrowser());
	}

	/**
	 * First try to create phantomJS. If that fails, try Chrome.
	 */
	public RemoteWebDriver newBrowser() {
		RemoteWebDriver driver;
		switch (getBrowserType()) {
		case FIREFOX:
			driver = new FirefoxDriver();
			break;
		case INTERNET_EXPLORER:
			driver = new InternetExplorerDriver();
			break;
		case CHROME:
			driver = new ChromeDriver();
			break;
		case PHANTOMJS:
			driver = new PhantomJSDriver();
			break;
		default:
			throw new IllegalStateException("Unsupported browsertype "
					+ getBrowserType());
		}

		/* Store the browser as a used browser so we can clean it up later. */
		usedBrowsers.add(driver);

		driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS)
				.pageLoadTimeout(30, TimeUnit.SECONDS)
				.setScriptTimeout(30, TimeUnit.SECONDS);

		driver.manage().deleteAllCookies();

		return driver;
	}

	@Override
	protected void after() {
		for (RemoteWebDriver browser : usedBrowsers) {
			try {

				/* Make sure we clean up properly. */
				if (!browser.toString().contains("(null)")) {
					if (getBrowserType() == BrowserType.PHANTOMJS) {
						// PhantomJS only quits the process on quit command
						browser.quit();
					} else {
						browser.close();
					}
				}

			} catch (RuntimeException e) {
				LOG.warn("Could not close the browser: {}", e.getMessage());
			}
		}
	}
}
