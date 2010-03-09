package com.fieldexpert.fbapi4j.session;

import static com.fieldexpert.fbapi4j.common.StringUtil.collectionToCommaDelimitedString;
import static java.util.Arrays.asList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import com.fieldexpert.fbapi4j.AllowedOperation;
import com.fieldexpert.fbapi4j.Case;
import com.fieldexpert.fbapi4j.CaseBuilder;
import com.fieldexpert.fbapi4j.Fbapi4j;
import com.fieldexpert.fbapi4j.Fbapi4jException;
import com.fieldexpert.fbapi4j.common.Assert;
import com.fieldexpert.fbapi4j.common.Attachment;
import com.fieldexpert.fbapi4j.common.StringUtil;
import com.fieldexpert.fbapi4j.common.Util;
import com.fieldexpert.fbapi4j.dispatch.Dispatch;
import com.fieldexpert.fbapi4j.dispatch.Request;
import com.fieldexpert.fbapi4j.dispatch.Response;
import com.fieldexpert.fbapi4j.http.Http;

class ConnectedSession implements Session {
	private String token;
	private String url;
	private Dispatch dispatch;
	private Util util;

	ConnectedSession(Dispatch dispatch) {
		this.dispatch = dispatch;
		this.util = new Util();
	}

	private void api() {
		if (url == null) {
			Response response = dispatch.invoke(Http.GET, util.url(dispatch.getEndpoint(), Fbapi4j.API_XML));
			Document doc = response.getDocument();
			url = util.children(doc).get("url");
			dispatch.setProperty("path", url);
		}
	}

	public void assign(Case bug) {
		Assert.notNull(bug.getNumber());
		// TODO Make sure this case is assignable
		Response resp = send(Fbapi4j.ASSIGN, events(bug));
		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	public void close() {
		Assert.notNull(token);
		dispatch.invoke(new Request(Fbapi4j.LOGOFF, util.map(Fbapi4j.TOKEN, token)));
		token = null;
	}

	public void close(Case bug) {
		Assert.notNull(token);
		if (!bug.getAllowedOperations().contains(AllowedOperation.CLOSE)) {
			throw new Fbapi4jException("This bug cannot be closed.");
		}
		Response resp = send(Fbapi4j.CLOSE, util.map(Fbapi4j.IX_BUG, bug.getNumber()));
		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> events(Case c) {
		try {
			Field events = c.getClass().getDeclaredField("_events");
			events.setAccessible(true);
			return new HashMap<String, Object>((Map<String, Object>) events.get(c));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void create(Case bug) {
		Assert.notNull(token);
		List<Attachment> attachments = bug.getAttachments();
		Map<String, Object> parameters = events(bug);
		Response resp;

		if (attachments == null) {
			resp = send(Fbapi4j.NEW, parameters);
		} else {
			resp = send(Fbapi4j.NEW, parameters, attachments);
		}

		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	private void updateCase(Case c, Map<String, String> data) {
		try {
			Method setNumber = c.getClass().getDeclaredMethod("setNumber", String.class);
			setNumber.setAccessible(true);
			setNumber.invoke(c, data.get(Fbapi4j.IX_BUG));

			List<String> allowed = StringUtil.commaDelimitedStringToSet(data.get(Fbapi4j.OPERATIONS));
			Set<AllowedOperation> operations = new HashSet<AllowedOperation>();

			for (String op : allowed) {
				operations.add(AllowedOperation.valueOf(op.toUpperCase()));
			}
			Field ops = c.getClass().getDeclaredField("allowedOperations");
			ops.setAccessible(true);
			ops.set(c, operations);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void edit(Case bug) {
		Assert.notNull(bug.getNumber());
		// TODO Make sure this case is editable
		Response resp = send(Fbapi4j.EDIT, events(bug));
		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	public Case getCase(int number) {
		String cols = collectionToCommaDelimitedString(asList(Fbapi4j.S_PROJECT, Fbapi4j.S_AREA, Fbapi4j.S_SCOUT_DESCRIPTION, Fbapi4j.S_TITLE, Fbapi4j.S_EVENT, Fbapi4j.EVENTS));
		Response resp = dispatch.invoke(new Request(Fbapi4j.SEARCH, util.map(Fbapi4j.TOKEN, token, Fbapi4j.QUERY, number, Fbapi4j.COLS, cols)));
		return new CaseBuilder(util, dispatch.getEndpoint(), token).build(resp.getDocument());
	}

	private Response send(String command, Map<String, Object> parameters) {
		return send(command, parameters, null);
	}

	private Response send(String command, Map<String, Object> parameters, List<Attachment> attachments) {
		parameters.put(Fbapi4j.TOKEN, token);

		Request request = new Request(command, parameters);
		if (attachments != null) {
			request.attach(attachments);
		}
		return dispatch.invoke(request);
	}

	void logon() {
		api();
		Response resp = dispatch.invoke(new Request(Fbapi4j.LOGON, util.map(Fbapi4j.EMAIL, dispatch.getEmail(), Fbapi4j.PASSWORD, dispatch.getPassword())));
		Document doc = resp.getDocument();
		token = util.children(doc).get("token");
	}

	public void reactivate(Case bug) {
		Assert.notNull(bug.getNumber());
		Response resp = send(Fbapi4j.REACTIVATE, events(bug));
		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	public void reopen(Case bug) {
		Assert.notNull(bug.getNumber());
		Response resp = send(Fbapi4j.REOPEN, events(bug));
		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	public void resolve(Case bug) {
		Assert.notNull(bug.getNumber());
		Response resp = send(Fbapi4j.RESOLVE, events(bug));
		updateCase(bug, util.data(resp.getDocument(), "case").get(0));
	}

	public void scout(Case bug) {
		Assert.notNull(token);
		List<Attachment> attachments = bug.getAttachments();
		Map<String, Object> parameters = events(bug);
		parameters.put(Fbapi4j.S_SCOUT_DESCRIPTION, bug.getTitle());

		if (attachments == null) {
			send(Fbapi4j.NEW, parameters);
		} else {
			send(Fbapi4j.NEW, parameters, attachments);
		}
	}

}
