import { useEffect, useState } from "react";
import { apiRequest } from "../../services/Api";

function InstituteRequests() {
  const role = localStorage.getItem("role");

  if (role !== "SUPER_ADMIN") {
    return <h1>Access Denied</h1>;
  }

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);

  // Fetch Requests
  const fetchRequests = async () => {
    try {
      const data = await apiRequest("/platform/institutes/requests");
      setRequests(data);
    } catch (err) {
      console.error(err);
      alert("Error fetching requests");
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  // Approve Request
  const approveRequest = async (id) => {
    if (!window.confirm("Approve this institute?")) return;

    try {
      setLoading(true);
      await apiRequest(`/platform/institutes/requests/${id}/approve`, "POST");
      fetchRequests();
    } catch (err) {
      console.error(err);
      alert("Error approving request");
    } finally {
      setLoading(false);
    }
  };

  // Reject Request (optional backend required)
  const rejectRequest = async (id) => {
    if (!window.confirm("Reject this institute?")) return;

    try {
      setLoading(true);
      await apiRequest(`/platform/institutes/requests/${id}/reject`, "POST");
      fetchRequests();
    } catch (err) {
      console.error(err);
      alert("Error rejecting request");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6 bg-gray-100 min-h-full">
      <h1 className="text-2xl font-bold mb-4">
        Institute Requests
      </h1>

      <div className="bg-white shadow rounded p-4">
        <table className="w-full">
          <thead className="bg-gray-200">
            <tr>
              <th className="p-2">Institute</th>
              <th>Admin Name</th>
              <th>Email</th>
              <th>Address</th>
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan="5" className="text-center p-4">
                  No Pending Requests
                </td>
              </tr>
            ) : (
              requests.map((req) => (
                <tr key={req.id} className="border-t text-center">
                  <td className="p-2">{req.instituteName}</td>
                  <td>{req.adminName}</td>
                  <td>{req.adminEmail}</td>
                  <td>{req.address}</td>

                  <td className="space-x-2">
                    <button
                      onClick={() => approveRequest(req.id)}
                      className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700"
                    >
                      Approve
                    </button>

                    <button
                      onClick={() => rejectRequest(req.id)}
                      className="bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700"
                    >
                      Reject
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default InstituteRequests;