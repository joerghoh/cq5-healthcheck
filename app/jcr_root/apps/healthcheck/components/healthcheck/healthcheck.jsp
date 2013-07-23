<%--
/* Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 --%>
<%@page import="de.joerghoh.cq5.healthcheck.Status,
                de.joerghoh.cq5.healthcheck.StatusService,
                org.apache.commons.lang3.StringEscapeUtils" session="false"%>
<%@include file="/libs/foundation/global.jsp"%>
<%-- CQ5 health check component. --%>
<%
    StatusService status = sling.getService(StatusService.class);
    Status systemStatus = null;
		 
	String[] categories = properties.get("categories", String[].class);
	int bundleNumberThreshold = properties.get("bundleNumberThreshold", 0);
	if (bundleNumberThreshold > 0) {
		systemStatus = status.getStatus(categories, bundleNumberThreshold);
	} else {
		systemStatus = status.getStatus(categories);
	}	 
    pageContext.setAttribute("systemStatus", systemStatus);
%>
<!DOCTYPE html>
<html>
<head>
    <title><%= currentPage.getTitle() == null ? StringEscapeUtils.escapeHtml4(currentPage.getName()) : StringEscapeUtils.escapeHtml4(currentPage.getTitle()) %></title>
    
    <style media="screen" type="text/css">
    	td.status {
    		text-align: center;
    	}
        td.level_warn {
            background-color: yellow;
        }
        td.level_critical {
            background-color: red;
        }
        td.level_ok{
            background-color: green;
        }
    </style>
</head>
<body>
    <h1>Overview</h1>
    <p><b>Overall Status:</b> ${systemStatus.status}</p>
    <c:if test="${not empty systemStatus.message}">
    <p><b>Caution:</b> ${systemStatus.message}</p>
    </c:if>
    
    <h1>Details</h1>
    <table border="1">
        <tr>
            <th>MBean</th>
            <th>Message</th>
            <th>Status</th>
        </tr>        
        <c:forEach var="status" items="${systemStatus.details}">
        <tr>
            <td>${status.provider}</td>
            <td>${status.message}&nbsp;</td>
            <td class="status level_${fn:toLowerCase(status.statusText)}">${status.statusText}</td>
        </tr>
        </c:forEach>
    </table>
</body>
</html>