<%@page import="de.joerghoh.cq5.healthcheck.*,java.util.List" session="false"%>
<%@include file="/libs/foundation/global.jsp"%>
<%-- CQ5 health check component. --%>
<%
    HealthStatusService status = sling.getService(HealthStatusService.class);
    SystemStatus systemStatus = status.getOverallStatus();
    pageContext.setAttribute("systemStatus", systemStatus);
%>
<!DOCTYPE html>
<html>
<head>
    <title>cq5-healthcheck</title>
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