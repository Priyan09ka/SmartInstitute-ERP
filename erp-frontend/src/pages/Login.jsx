import { useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../services/Api";
import { useNavigate } from "react-router-dom";
import { getDeviceId } from "../utils/device";

function Login(){

const [email,setEmail] = useState("");
const [password,setPassword] = useState("");
const navigate = useNavigate();

const[loading,setLoading]=useState(false);

const DASHBOARD_PATH_BY_ROLE = {
  SUPER_ADMIN: "/super-admin",
  INSTITUTE_ADMIN: "/institute-admin",
  PRINCIPAL: "/principal",
  TEACHER: "/teacher",
  STUDENT: "/student",
};


const handleLogin = async () => {
  if(!email || !password){
    alert("Please enter email and password");
    return;
  }
  setLoading(true);
  try {
    const data = await apiRequest("/auth/login", "POST", {
      email,
      password,
      deviceId: getDeviceId(),
      userAgent: navigator.userAgent
    });

    if (!data?.success) {
      alert(data?.message || "Login failed");
      return;
    }

    localStorage.setItem("token", data.token);
    localStorage.setItem("role", data.role);

    navigate(DASHBOARD_PATH_BY_ROLE[data.role] || "/dashboard");

  } catch (err) {
    alert(err?.message || "Login failed");
  }
  finally{
    setLoading(false);
  }
};
return (

<div className="flex items-center justify-center min-h-screen bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500">

<div className="bg-white p-8 rounded-2xl shadow-xl w-96">

<h1 className="text-3xl font-bold text-center text-gray-800 mb-2">
School ERP
</h1>

<p className="text-center text-gray-500 mb-6">
Login to your account
</p>

<input
value={email}
onChange={(e)=>setEmail(e.target.value)}
type="email"
placeholder="Enter email"
className="w-full p-3 border border-gray-300 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-indigo-400"
/>

<input
value={password}
onChange={(e)=>setPassword(e.target.value)}
type="password"
placeholder="Enter password"
className="w-full p-3 border border-gray-300 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-indigo-400"
/>

<div className="flex justify-end mb-6">
  <Link to="/forgot-password" className="text-sm text-indigo-600 hover:underline">
    Forgot Password?
  </Link>
</div>

<button
onClick={handleLogin}
disabled={loading}
className="w-full bg-indigo-600 text-white p-3 rounded-lg font-semibold hover:bg-indigo-700 transition duration-300"
>
{loading?"Logging in...":"Login"}
</button>

<p className="text-center text-xs text-gray-400 mt-4 max-w-sm mx-auto leading-relaxed">
  Are you a <span className="text-gray-600">school or institute</span> and need platform access?{" "}
  <Link to="/institute-request" className="text-indigo-600 hover:underline font-medium">
    Request institute onboarding
  </Link>
  <span className="block mt-1 text-gray-400">Students: sign in with the email your school gave you.</span>
</p>

</div>

</div>

)
}

export default Login