import Navbar from "../components/Navbar"
import Sidebar from "../components/Sidebar"

function Dashboard(){
    const role=localStorage.getItem("role")

return(

<div>

<Navbar/>

<div className="flex">

<Sidebar role={role}/>

<div className="p-6 flex-1 bg-gray-100 min-h-screen">

<h1 className="text-2xl font-bold mb-6">
Dashboard
</h1>

<div className="grid grid-cols-3 gap-6">

<div className="bg-white p-6 rounded shadow">
<h2 className="text-gray-500">Students</h2>
<p className="text-2xl font-bold">120</p>
</div>

<div className="bg-white p-6 rounded shadow">
<h2 className="text-gray-500">Teachers</h2>
<p className="text-2xl font-bold">25</p>
</div>

<div className="bg-white p-6 rounded shadow">
<h2 className="text-gray-500">Subjects</h2>
<p className="text-2xl font-bold">15</p>
</div>

</div>

</div>

</div>

</div>

)

}

export default Dashboard