// Proxy configuration
var config = {
    mode: "fixed_servers",
    rules: {
        singleProxy: {
            scheme: "http",
            host: "gw.dataimpulse.com", // Replace with your proxy hostname
            port: 823 // Replace with your proxy port
        },
        bypassList: ["localhost"] // Bypass proxy for localhost
    }
};

// Set the proxy configuration
chrome.proxy.settings.set({ value: config, scope: "regular" }, function () {
    console.log("Proxy configuration applied.");
});

// Handle proxy authentication
function callbackFn(details) {
    return {
        authCredentials: {
            username: "b05aa674a822ff775e48__cr.us", // Replace with your proxy username
            password: "13db01354c95c9e5" // Replace with your proxy password
        }
    };
}

// Listen for authentication requests
chrome.webRequest.onAuthRequired.addListener(
    callbackFn,
    { urls: ["<all_urls>"] }, // Apply to all URLs
    ["blocking"]
);