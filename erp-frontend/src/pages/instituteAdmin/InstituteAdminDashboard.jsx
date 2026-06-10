import InstitutePortalLayout from "../../components/InstitutePortalLayout";
import InstitutePortalDashboard from "./InstitutePortalDashboard";

function InstituteAdminDashboard() {
  return (
    <InstitutePortalLayout portalTitle="Institute Admin">
      <InstitutePortalDashboard mode="admin" />
    </InstitutePortalLayout>
  );
}

export default InstituteAdminDashboard;
