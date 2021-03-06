package com.fieldexpert.fbapi4j;

import java.util.List;

import com.fieldexpert.fbapi4j.dispatch.Dispatch;

class MuxSession implements Session {
	private ConnectedSession connected;
	private Session disconnected;
	private Session state;

	public MuxSession(Dispatch dispatch) {
		this.disconnected = new DisconnectedSession();
		this.connected = new ConnectedSession(dispatch);
		this.state = disconnected;
	}

	public void close() {
		if (state != disconnected) {
			state.close();
		}
		disconnect();
	}

	public void close(Case bug) {
		connect();
		state.close(bug);
	}

	private void connect() {
		if (state == disconnected) {
			connected.logon();
			state = connected;
		}
		SessionFactory.setCurrentSession(state);
	}

	private void disconnect() {
		if (state == connected) {
			state = disconnected;
		}
		SessionFactory.setCurrentSession(null);
	}

	public void edit(Case bug) {
		connect();
		state.edit(bug);
	}

	public void reactivate(Case bug) {
		connect();
		state.reactivate(bug);
	}

	public void reopen(Case bug) {
		connect();
		state.reopen(bug);
	}

	public void resolve(Case bug) {
		connect();
		state.resolve(bug);
	}

	public void scout(Case bug) {
		connect();
		state.scout(bug);
	}

	public void create(Entity t) {
		connect();
		state.create(t);
	}

	public <T extends Entity> List<T> findAll(Class<T> clazz) {
		connect();
		return state.findAll(clazz);
	}

	public <T extends Entity> T get(Class<T> clazz, Integer id) {
		connect();
		return state.get(clazz, id);
	}

	public List<Case> query(String... criterion) {
		connect();
		return state.query(criterion);
	}

	public <T extends Entity> T get(Class<T> clazz, String name) {
		connect();
		return state.get(clazz, name);
	}

}
