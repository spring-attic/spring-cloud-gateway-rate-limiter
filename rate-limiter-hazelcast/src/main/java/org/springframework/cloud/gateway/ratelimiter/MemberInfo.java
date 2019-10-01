package org.springframework.cloud.gateway.ratelimiter;

import java.util.Objects;

public class MemberInfo {

	private String host;
	private int port;

	public MemberInfo(String host) {
		this.host = host;
	}

	public MemberInfo(String host, int port) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberInfo that = (MemberInfo) o;
		return port == that.port &&
				Objects.equals(host, that.host);
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port);
	}

	@Override
	public String toString() {
		return "MemberInfo{" +
				"host='" + host + '\'' +
				", port=" + port +
				'}';
	}
}
