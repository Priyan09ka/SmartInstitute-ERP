export const getDeviceId = () => {

  let deviceId = localStorage.getItem("deviceId");

  if (!deviceId) {
    deviceId = crypto.randomUUID(); // generate unique id
    localStorage.setItem("deviceId", deviceId);
  }

  return deviceId;
};