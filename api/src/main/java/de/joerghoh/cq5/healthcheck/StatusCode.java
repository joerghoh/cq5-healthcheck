/*
 * Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.joerghoh.cq5.healthcheck;

/**
 * Status value definitions are taken from
 * http://nagiosplug.sourceforge.net/developer-guidelines.html#AEN76 and are the
 * values which are defined by the popular Nagios monitoring system for the
 * communication with its plugins.
 */
public enum StatusCode {
	OK("OK"),

	WARN("WARN"),

	CRITICAL("CRITICAL"),

	UNKNOWN("ARCHIVE");

	private String msg;

	StatusCode(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return msg;
	}
}
