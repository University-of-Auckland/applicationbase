<%@ page contentType="text/html"%>

  <!-- THIS IS USED FOR TEST PURPOSES ONLY AND OVERRIDEN VIA BRANDING IN HOST APPLICATION -->

  An important task <strong>requiring your approval</strong> is waiting for your input within <g:link controller="workflowApproval" action="list">INTEGRATION TEST workflows</g:link>.
  <br><br>

  <h4 class="h4">Task Details</h4>
  <table>
    <tr>
      <td>
        Name:
      </td>
      <td>
        ${fieldValue(bean: taskInstance, field: "task.name")} (ID: ${fieldValue(bean: taskInstance, field: "id")})
      </td>
    </tr>
    <tr>
      <td>
        Description:
      </td>
      <td>
        ${fieldValue(bean: taskInstance, field: "task.description")}
      </td>
    </tr>
    <tr>
      <td>
        Process Instance:
      </td>
      <td>
        ${fieldValue(bean: taskInstance, field: "processInstance.description")}
      </td>
    </tr>
  </table>

  <br><br>
  Please <g:link controller="workflowApproval" action="list"><strong>action approvals</strong></g:link> waiting on your input at your earliest convenience.
