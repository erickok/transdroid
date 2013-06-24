/*
 * Taken from the 'Learning Android' project, released as Public Domain software at
 * http://github.com/digitalspaghetti/learning-android and modified heavily for Transdroid
 */
package org.transdroid.core.rssparser;

import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RssParser extends DefaultHandler {

	private String urlString;
	private Channel channel;
	private StringBuilder text;
	private Item item;
	private boolean imageStatus;

	/**
	 * The constructor for the RSS parser; call {@link #parse()} to synchronously create an HTTP connection and parse
	 * the RSS feed contents. The results can be retrieved with {@link #getChannel()}.
	 * @param url
	 */
	public RssParser(String url) {
		this.urlString = url;
		this.text = new StringBuilder();
	}

	/**
	 * Returns the loaded RSS feed as channel which contains the individual {@link Item}s
	 * @return A channel object that ocntains the feed details and individual items
	 */
	public Channel getChannel() {
		return this.channel;
	}

	/**
	 * Initialises an HTTP connection, retrieves the content and parses the RSS feed as standard XML.
	 * @throws ParserConfigurationException Thrown if the SX parser is not working corectly
	 * @throws SAXException Thrown if the SAX parser can encounters non-standard XML content
	 * @throws IOException Thrown if the RSS feed content can not be retrieved, such as when no connection is available
	 */
	public void parse() throws ParserConfigurationException, SAXException, IOException {

		DefaultHttpClient httpclient = initialise();
		HttpResponse result = httpclient.execute(new HttpGet(urlString));
		SAXParserFactory spf = SAXParserFactory.newInstance();
		if (spf != null) {
			SAXParser sp = spf.newSAXParser();
			sp.parse(result.getEntity().getContent(), this);
		}

	}

	private DefaultHttpClient initialise() {

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));

		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, 5000);
		HttpConnectionParams.setSoTimeout(httpparams, 5000);
		DefaultHttpClient httpclient = new DefaultHttpClient(new ThreadSafeClientConnManager(httpparams, registry),
				httpparams);

		httpclient.addRequestInterceptor(HttpHelper.gzipRequestInterceptor);
		httpclient.addResponseInterceptor(HttpHelper.gzipResponseInterceptor);

		return httpclient;

	}

	/**
	 * By default creates a standard Item (with title, description and links), which may to overridden to add more data
	 * (i.e. custom tags that a feed may supply).
	 * @return A possibly decorated Item instance
	 */
	protected Item createNewItem() {
		return new Item();
	}

	@Override
	public final void startElement(String uri, String localName, String qName, Attributes attributes) {

		/** First lets check for the channel */
		if (localName.equalsIgnoreCase("channel")) {
			this.channel = new Channel();
		}

		/** Now lets check for an item */
		if (localName.equalsIgnoreCase("item") && (this.channel != null)) {
			this.item = createNewItem();
			this.channel.addItem(this.item);
		}

		/** Now lets check for an image */
		if (localName.equalsIgnoreCase("image") && (this.channel != null)) {
			this.imageStatus = true;
		}

		/** Checking for a enclosure */
		if (localName.equalsIgnoreCase("enclosure")) {
			/** Lets check we are in an item */
			if (this.item != null && attributes != null && attributes.getLength() > 0) {
				if (attributes.getValue("url") != null) {
					this.item.setEnclosureUrl(attributes.getValue("url").trim());
				}
				if (attributes.getValue("type") != null) {
					this.item.setEnclosureType(attributes.getValue("type"));
				}
				if (attributes.getValue("length") != null) {
					this.item.setEnclosureLength(Long.parseLong(attributes.getValue("length")));
				}
			}
		}

	}

	/**
	 * This is where we actually parse for the elements contents
	 */
	@SuppressWarnings("deprecation")
	public final void endElement(String uri, String localName, String qName) {
		/** Check we have an RSS Feed */
		if (this.channel == null) {
			return;
		}

		/** Check are at the end of an item */
		if (localName.equalsIgnoreCase("item")) {
			this.item = null;
		}

		/** Check we are at the end of an image */
		if (localName.equalsIgnoreCase("image"))
			this.imageStatus = false;

		/** Now we need to parse which title we are in */
		if (localName.equalsIgnoreCase("title")) {
			/** We are an item, so we set the item title */
			if (this.item != null) {
				this.item.setTitle(this.text.toString().trim());
				/** We are in an image */
			} else {
				this.channel.setTitle(this.text.toString().trim());
			}
		}

		/** Now we are checking for a link */
		if (localName.equalsIgnoreCase("link")) {
			/** Check we are in an item **/
			if (this.item != null) {
				this.item.setLink(this.text.toString().trim());
				/** Check we are in an image */
			} else if (this.imageStatus) {
				this.channel.setImage(this.text.toString().trim());
				/** Check we are in a channel */
			} else {
				this.channel.setLink(this.text.toString().trim());
			}
		}

		/** Checking for a description */
		if (localName.equalsIgnoreCase("description")) {
			/** Lets check we are in an item */
			if (this.item != null) {
				this.item.setDescription(this.text.toString().trim());
				/** Lets check we are in the channel */
			} else {
				this.channel.setDescription(this.text.toString().trim());
			}
		}

		/** Checking for a pubdate */
		if (localName.equalsIgnoreCase("pubDate")) {
			/** Lets check we are in an item */
			if (this.item != null) {
				try {
					this.item.setPubdate(new Date(Date.parse(this.text.toString().trim())));
				} catch (Exception e) {
					// Date is malformed (not parsable by Date.parse)
				}
				/** Lets check we are in the channel */
			} else {
				try {
					this.channel.setPubDate(new Date(Date.parse(this.text.toString().trim())));
				} catch (Exception e) {
					// Date is malformed (not parsable by Date.parse)
				}
			}
		}

		/** Check for the category */
		if (localName.equalsIgnoreCase("category") && (this.item != null)) {
			this.channel.addCategory(this.text.toString().trim());
		}

		addAdditionalData(localName, this.item, this.text.toString());

		this.text.setLength(0);
	}

	/**
	 * May be overridden to add additional data from tags that are not standard in RSS. Not used by this default RSS
	 * style parser. Usually used in conjunction with {@link #createNewItem()}.
	 * @param localName The tag name
	 * @param item The Item we are currently parsing
	 * @param text The new text content
	 */
	protected void addAdditionalData(String localName, Item item, String text) {
	}

	@Override
	public final void characters(char[] ch, int start, int length) {
		this.text.append(ch, start, length);
	}

}