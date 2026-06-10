function Navbar(){

return(

<div className="h-16 bg-white shadow flex items-center justify-between px-6">

<h1 className="text-xl font-bold text-indigo-600">
School ERP
</h1>

<div className="flex items-center gap-4">

<span className="text-gray-600">
Admin
</span>

<button
className="bg-red-500 text-white px-3 py-1 rounded"
onClick={()=>{
localStorage.clear()
window.location.href="/"
}}
>
Logout
</button>

</div>

</div>

)

}

export default Navbar