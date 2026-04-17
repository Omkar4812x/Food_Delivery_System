var appState = {
  user: null,
  restaurants: [],
  menu: [],
  orders: [],
  users: [],
  cart: []
};

document.addEventListener("DOMContentLoaded", function () {
  setupAuthTabs();
  setupLogin();
  setupRegister();
  setupLogout();

  if (document.body.getAttribute("data-page") === "admin") {
    initAdmin();
  }
  if (document.body.getAttribute("data-page") === "customer") {
    initCustomer();
  }
});

function setupAuthTabs() {
  var buttons = document.querySelectorAll("[data-auth-tab]");
  for (var i = 0; i < buttons.length; i++) {
    buttons[i].addEventListener("click", function () {
      for (var j = 0; j < buttons.length; j++) {
        buttons[j].classList.remove("active");
      }
      this.classList.add("active");
      var tab = this.getAttribute("data-auth-tab");
      toggleElement("loginForm", tab !== "login");
      toggleElement("registerForm", tab !== "register");
      setMessage("");
    });
  }
}

function setupLogin() {
  var form = document.getElementById("loginForm");
  if (!form || form.getAttribute("data-inline-login") === "enabled") {
    return;
  }
  form.addEventListener("submit", function (event) {
    event.preventDefault();
    setMessage("Checking credentials...");
    postForm("LoginController", new FormData(form), function (user) {
      window.location.href = user.role === "admin" ? "admindashboard.html" : "userdashboard.html";
    }, function () {
      setMessage("Invalid email or password.", true);
    });
  });
}

function setupRegister() {
  var form = document.getElementById("registerForm");
  if (!form) {
    return;
  }
  form.addEventListener("submit", function (event) {
    event.preventDefault();
    postForm("RegisterController", new FormData(form), function () {
      setMessage("Account created. Login with your new email and password.");
      form.reset();
      var loginTab = document.querySelector('[data-auth-tab="login"]');
      if (loginTab) {
        loginTab.click();
      }
    }, function () {
      setMessage("Registration failed. Email may already exist.", true);
    });
  });
}

function setupLogout() {
  var button = document.getElementById("logoutBtn");
  if (!button) {
    return;
  }
  button.addEventListener("click", function () {
    postForm("api/logout", new FormData(), function () {
      window.location.href = "login.html";
    }, function () {
      window.location.href = "login.html";
    });
  });
}

function initAdmin() {
  getJson("api/session", function (user) {
    appState.user = user;
    if (!user || user.role !== "admin") {
      window.location.href = "login.html";
      return;
    }
    bindAdminForms();
    loadAdminData();
  }, function () {
    window.location.href = "login.html";
  });
}

function loadAdminData() {
  getJson("api/stats", function (stats) {
    renderStats(stats);
  });
  getJson("api/restaurants", function (restaurants) {
    appState.restaurants = restaurants;
    renderRestaurantSelects();
    renderAdminRestaurants();
  });
  getJson("api/menu", function (menu) {
    appState.menu = menu;
    renderAdminMenu();
  });
  getJson("api/orders", function (orders) {
    appState.orders = orders;
    renderAdminOrders();
  });
  getJson("api/users", function (users) {
    appState.users = users;
    renderUsers();
  });
}

function bindAdminForms() {
  var restaurantForm = document.getElementById("restaurantForm");
  if (restaurantForm) {
    restaurantForm.addEventListener("submit", function (event) {
      event.preventDefault();
      if (!validateRequiredFields(restaurantForm, ["name", "address", "contact", "cuisines"])) {
        return;
      }
      var idField = getFormField(restaurantForm, "id");
      var id = idField ? idField.value : "";
      postForm(id ? "api/restaurants/update" : "api/restaurants", buildFormData(restaurantForm), function () {
        toast(id ? "Restaurant updated" : "Restaurant added");
        restaurantForm.reset();
        if (idField) {
          idField.value = "";
        }
        loadAdminData();
      }, function (xhr) {
        toast(readError(xhr, "Restaurant save failed."), true);
      });
    });
  }

  var menuForm = document.getElementById("menuForm");
  if (menuForm) {
    menuForm.addEventListener("submit", function (event) {
      event.preventDefault();
      if (!validateRequiredFields(menuForm, ["restaurantId", "name", "price"])) {
        return;
      }
      var idField = getFormField(menuForm, "id");
      var vegField = getFormField(menuForm, "veg");
      var availableField = getFormField(menuForm, "available");
      var id = idField ? idField.value : "";
      var data = buildFormData(menuForm);
      data.set("veg", vegField && vegField.checked ? "true" : "false");
      data.set("available", availableField && availableField.checked ? "true" : "false");
      postForm(id ? "api/menu/update" : "api/menu", data, function () {
        toast(id ? "Menu item updated" : "Menu item added");
        menuForm.reset();
        if (idField) {
          idField.value = "";
        }
        if (vegField) {
          vegField.checked = true;
        }
        if (availableField) {
          availableField.checked = true;
        }
        loadAdminData();
      }, function (xhr) {
        toast(readError(xhr, "Menu save failed."), true);
      });
    });
  }

  bindClick("resetRestaurantForm", function () {
    restaurantForm.reset();
    var idField = getFormField(restaurantForm, "id");
    if (idField) {
      idField.value = "";
    }
  });
  bindClick("resetMenuForm", function () {
    menuForm.reset();
    var idField = getFormField(menuForm, "id");
    var vegField = getFormField(menuForm, "veg");
    var availableField = getFormField(menuForm, "available");
    if (idField) {
      idField.value = "";
    }
    if (vegField) {
      vegField.checked = true;
    }
    if (availableField) {
      availableField.checked = true;
    }
  });
  bindInput("restaurantSearch", renderAdminRestaurants);
  bindInput("menuSearch", renderAdminMenu);
  bindInput("orderStatusFilter", renderAdminOrders);
}

function renderStats(stats) {
  var target = document.getElementById("statsGrid");
  if (!target) {
    return;
  }
  var items = [
    ["Restaurants", stats.restaurants],
    ["Menu items", stats.menuItems],
    ["Orders", stats.orders],
    ["Customers", stats.customers],
    ["Revenue", money(stats.revenue)],
    ["Active orders", stats.pendingOrders]
  ];
  var html = "";
  for (var i = 0; i < items.length; i++) {
    html += '<article class="stat-tile"><span>' + items[i][0] + '</span><strong>' + items[i][1] + '</strong></article>';
  }
  target.innerHTML = html;
}

function renderRestaurantSelects() {
  var select = document.getElementById("menuRestaurantSelect");
  if (!select) {
    return;
  }
  var html = "";
  for (var i = 0; i < appState.restaurants.length; i++) {
    var r = appState.restaurants[i];
    html += '<option value="' + r.id + '">' + escapeHtml(r.name) + '</option>';
  }
  select.innerHTML = html;
}

function renderAdminRestaurants() {
  var target = document.getElementById("restaurantTableBody");
  if (!target) {
    return;
  }
  var search = elementValue("restaurantSearch").toLowerCase();
  var html = "";
  for (var i = 0; i < appState.restaurants.length; i++) {
    var r = appState.restaurants[i];
    var haystack = (r.name + " " + r.cuisines + " " + r.address).toLowerCase();
    if (haystack.indexOf(search) === -1) {
      continue;
    }
    html += '<tr>'
      + '<td><strong>' + escapeHtml(r.name) + '</strong><br><small>' + escapeHtml(r.address) + '</small></td>'
      + '<td>' + escapeHtml(r.cuisines) + '</td>'
      + '<td><span class="badge ' + (r.status === "open" ? "green" : "red") + '">' + escapeHtml(r.status) + '</span></td>'
      + '<td>' + r.itemCount + '</td>'
      + '<td class="actions"><button type="button" onclick="editRestaurant(' + r.id + ')">Edit</button>'
      + '<button type="button" class="danger" onclick="deleteRestaurant(' + r.id + ')">Delete</button></td>'
      + '</tr>';
  }
  target.innerHTML = html || emptyRow(5);
}

function renderAdminMenu() {
  var target = document.getElementById("menuTableBody");
  if (!target) {
    return;
  }
  var search = elementValue("menuSearch").toLowerCase();
  var html = "";
  var items = appState.menu.slice(0);
  items.sort(function (a, b) {
    return Number(b.id || 0) - Number(a.id || 0);
  });
  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    var haystack = (item.name + " " + item.restaurantName + " " + item.category).toLowerCase();
    if (haystack.indexOf(search) === -1) {
      continue;
    }
    html += '<tr>'
      + '<td><img class="menu-thumb" src="' + escapeHtml(item.imageUrl || defaultFoodImage()) + '" alt="' + escapeHtml(item.name) + '"></td>'
      + '<td><strong>' + escapeHtml(item.name) + '</strong><br><small>' + escapeHtml(item.description || "") + '</small></td>'
      + '<td>' + escapeHtml(item.restaurantName) + '</td>'
      + '<td>' + money(item.price) + '</td>'
      + '<td><span class="badge ' + (item.veg ? "green" : "red") + '">' + (item.veg ? "Veg" : "Non-veg") + '</span></td>'
      + '<td class="actions"><button type="button" onclick="editMenuItem(' + item.id + ')">Edit</button>'
      + '<button type="button" class="danger" onclick="deleteMenuItem(' + item.id + ')">Delete</button></td>'
      + '</tr>';
  }
  target.innerHTML = html || emptyRow(6);
}

function renderAdminOrders() {
  var target = document.getElementById("adminOrders");
  if (!target) {
    return;
  }
  var filter = elementValue("orderStatusFilter");
  var html = "";
  for (var i = 0; i < appState.orders.length; i++) {
    var order = appState.orders[i];
    if (filter && order.status !== filter) {
      continue;
    }
    html += orderCard(order, true);
  }
  target.innerHTML = html || emptyState("No orders found.");
}

function renderUsers() {
  var target = document.getElementById("usersTableBody");
  if (!target) {
    return;
  }
  var html = "";
  for (var i = 0; i < appState.users.length; i++) {
    var u = appState.users[i];
    html += '<tr><td>' + escapeHtml(u.name) + '</td><td>' + escapeHtml(u.email) + '</td><td>'
      + escapeHtml(u.phone || "-") + '</td><td>' + escapeHtml(u.address || "-")
      + '</td><td><span class="badge">' + escapeHtml(u.role) + '</span></td></tr>';
  }
  target.innerHTML = html || emptyRow(5);
}

function editRestaurant(id) {
  var r = findById(appState.restaurants, id);
  var form = document.getElementById("restaurantForm");
  if (!r || !form) {
    return;
  }
  setFormFieldValue(form, "id", r.id);
  setFormFieldValue(form, "name", r.name);
  setFormFieldValue(form, "address", r.address);
  setFormFieldValue(form, "contact", r.contact);
  setFormFieldValue(form, "cuisines", r.cuisines);
  setFormFieldValue(form, "rating", r.rating);
  setFormFieldValue(form, "deliveryTime", r.deliveryTime);
  setFormFieldValue(form, "status", r.status);
  form.scrollIntoView({ behavior: "smooth", block: "start" });
}

function deleteRestaurant(id) {
  if (!confirm("Delete this restaurant and its menu items?")) {
    return;
  }
  var data = new FormData();
  data.set("id", id);
  postForm("api/restaurants/delete", data, function () {
    toast("Restaurant deleted");
    loadAdminData();
  }, function (xhr) {
    toast(readError(xhr, "Restaurant delete failed."), true);
  });
}

function editMenuItem(id) {
  var item = findById(appState.menu, id);
  var form = document.getElementById("menuForm");
  if (!item || !form) {
    return;
  }
  setFormFieldValue(form, "id", item.id);
  setFormFieldValue(form, "restaurantId", item.restaurantId);
  setFormFieldValue(form, "name", item.name);
  setFormFieldValue(form, "description", item.description || "");
  setFormFieldValue(form, "imageUrl", item.imageUrl || "");
  setFormFieldValue(form, "price", item.price);
  setFormFieldValue(form, "category", item.category || "");
  setFormFieldChecked(form, "veg", item.veg);
  setFormFieldChecked(form, "available", item.available);
  form.scrollIntoView({ behavior: "smooth", block: "start" });
}

function deleteMenuItem(id) {
  if (!confirm("Delete this menu item?")) {
    return;
  }
  var data = new FormData();
  data.set("id", id);
  postForm("api/menu/delete", data, function () {
    toast("Menu item deleted");
    loadAdminData();
  }, function (xhr) {
    toast(readError(xhr, "Menu delete failed."), true);
  });
}

function updateOrderStatus(id, status) {
  var data = new FormData();
  data.set("id", id);
  data.set("status", status);
  postForm("api/orders/status", data, function () {
    toast("Order status updated");
    loadAdminData();
  }, function (xhr) {
    toast(readError(xhr, "Status update failed."), true);
  });
}

function initCustomer() {
  getJson("api/session", function (user) {
    appState.user = user;
    if (!user || user.role !== "customer") {
      window.location.href = "login.html";
      return;
    }
    var greeting = document.getElementById("userGreeting");
    if (greeting) {
      greeting.textContent = "Hi " + user.name + ", your food is a few clicks away.";
    }
    var form = document.getElementById("orderForm");
    if (form) {
      form.elements.customerName.value = user.name || "";
      form.elements.customerPhone.value = user.phone || "";
      form.elements.deliveryAddress.value = user.address || "";
    }
    bindCustomer();
    loadCustomerData();
  }, function () {
    window.location.href = "login.html";
  });
}

function bindCustomer() {
  bindInput("customerSearch", renderCustomerRestaurants);
  bindInput("customerRestaurantFilter", renderCustomerMenu);
  var form = document.getElementById("orderForm");
  if (form) {
    form.addEventListener("submit", placeOrder);
  }
}

function loadCustomerData() {
  getJson("api/restaurants", function (restaurants) {
    appState.restaurants = [];
    for (var i = 0; i < restaurants.length; i++) {
      if (restaurants[i].status === "open") {
        appState.restaurants.push(restaurants[i]);
      }
    }
    renderCustomerFilter();
    renderCustomerRestaurants();
  });
  getJson("api/menu", function (menu) {
    appState.menu = [];
    for (var i = 0; i < menu.length; i++) {
      if (menu[i].available) {
        appState.menu.push(menu[i]);
      }
    }
    renderCustomerMenu();
  });
  getJson("api/orders?userId=" + appState.user.id, function (orders) {
    appState.orders = orders;
    renderCustomerOrders();
  });
  renderCart();
}

function renderCustomerFilter() {
  var select = document.getElementById("customerRestaurantFilter");
  if (!select) {
    return;
  }
  var html = '<option value="">All restaurants</option>';
  for (var i = 0; i < appState.restaurants.length; i++) {
    html += '<option value="' + appState.restaurants[i].id + '">' + escapeHtml(appState.restaurants[i].name) + '</option>';
  }
  select.innerHTML = html;
}

function renderCustomerRestaurants() {
  var target = document.getElementById("restaurantCards");
  if (!target) {
    return;
  }
  var search = elementValue("customerSearch").toLowerCase();
  var html = "";
  for (var i = 0; i < appState.restaurants.length; i++) {
    var r = appState.restaurants[i];
    var haystack = (r.name + " " + r.cuisines + " " + r.address).toLowerCase();
    if (haystack.indexOf(search) === -1) {
      continue;
    }
    html += '<article class="restaurant-card"><div><span class="badge green">' + r.rating + ' star</span>'
      + '<h3>' + escapeHtml(r.name) + '</h3><p>' + escapeHtml(r.cuisines) + '</p><small>'
      + escapeHtml(r.address) + ' - ' + escapeHtml(r.deliveryTime) + '</small></div>'
      + '<button type="button" onclick="filterRestaurant(' + r.id + ')">View menu</button></article>';
  }
  target.innerHTML = html || emptyState("No restaurants found.");
}

function filterRestaurant(id) {
  var select = document.getElementById("customerRestaurantFilter");
  if (select) {
    select.value = id;
  }
  renderCustomerMenu();
  var menu = document.getElementById("menu");
  if (menu) {
    menu.scrollIntoView({ behavior: "smooth" });
  }
}

function renderCustomerMenu() {
  var target = document.getElementById("menuCards");
  if (!target) {
    return;
  }
  var restaurantId = elementValue("customerRestaurantFilter");
  var html = "";
  for (var i = 0; i < appState.menu.length; i++) {
    var item = appState.menu[i];
    if (restaurantId && String(item.restaurantId) !== String(restaurantId)) {
      continue;
    }
    html += '<article class="menu-card"><img class="menu-card-image" src="' + escapeHtml(item.imageUrl || defaultFoodImage()) + '" alt="' + escapeHtml(item.name) + '"><div><span class="badge ' + (item.veg ? "green" : "red") + '">'
      + (item.veg ? "Veg" : "Non-veg") + '</span><h3>' + escapeHtml(item.name) + '</h3><p>'
      + escapeHtml(item.description || item.category || "") + '</p><small>' + escapeHtml(item.restaurantName)
      + '</small></div><div class="menu-card-footer"><strong>' + money(item.price)
      + '</strong><button type="button" onclick="addToCart(' + item.id + ')">Add</button></div></article>';
  }
  target.innerHTML = html || emptyState("No menu items found.");
}

function addToCart(id) {
  var existing = findById(appState.cart, id);
  if (existing) {
    existing.quantity += 1;
  } else {
    var item = findById(appState.menu, id);
    if (!item) {
      return;
    }
    appState.cart.push({
      id: item.id,
      name: item.name,
      restaurantName: item.restaurantName,
      price: item.price,
      quantity: 1
    });
  }
  renderCart();
  toast("Added to cart");
}

function changeQuantity(id, delta) {
  var item = findById(appState.cart, id);
  if (!item) {
    return;
  }
  item.quantity += delta;
  if (item.quantity <= 0) {
    var next = [];
    for (var i = 0; i < appState.cart.length; i++) {
      if (appState.cart[i].id !== id) {
        next.push(appState.cart[i]);
      }
    }
    appState.cart = next;
  }
  renderCart();
}

function renderCart() {
  var target = document.getElementById("cartItems");
  var total = document.getElementById("cartTotal");
  if (!target || !total) {
    return;
  }
  var html = "";
  for (var i = 0; i < appState.cart.length; i++) {
    var item = appState.cart[i];
    html += '<div class="cart-line"><div><strong>' + escapeHtml(item.name) + '</strong><small>'
      + money(item.price) + ' - ' + escapeHtml(item.restaurantName) + '</small></div><div class="qty">'
      + '<button type="button" onclick="changeQuantity(' + item.id + ', -1)">-</button><span>' + item.quantity
      + '</span><button type="button" onclick="changeQuantity(' + item.id + ', 1)">+</button></div></div>';
  }
  target.innerHTML = html || '<p class="muted">Your cart is empty.</p>';
  total.textContent = money(cartTotal());
}

function placeOrder(event) {
  event.preventDefault();
  if (!appState.cart.length) {
    toast("Add items before placing order", true);
    return;
  }
  var form = event.currentTarget;
  var items = [];
  for (var i = 0; i < appState.cart.length; i++) {
    items.push({ id: appState.cart[i].id, quantity: appState.cart[i].quantity });
  }
  var payload = {
    customerName: form.elements.customerName.value,
    customerPhone: form.elements.customerPhone.value,
    deliveryAddress: form.elements.deliveryAddress.value,
    paymentMethod: form.elements.paymentMethod.value,
    items: items
  };
  postJson("api/orders/place", payload, function () {
    appState.cart = [];
    toast("Order placed successfully");
    loadCustomerData();
  }, function (xhr) {
    toast(readError(xhr, "Order failed. Please try again."), true);
  });
}

function renderCustomerOrders() {
  var target = document.getElementById("customerOrders");
  if (!target) {
    return;
  }
  var html = "";
  for (var i = 0; i < appState.orders.length; i++) {
    html += orderCard(appState.orders[i], false);
  }
  target.innerHTML = html || emptyState("No orders yet.");
}

function orderCard(order, admin) {
  var items = [];
  for (var i = 0; i < order.items.length; i++) {
    items.push(escapeHtml(order.items[i].itemName) + " x " + order.items[i].quantity);
  }
  var statusSelect = "";
  if (admin) {
    var statuses = ["pending", "preparing", "out_for_delivery", "delivered", "cancelled"];
    statusSelect = '<select onchange="updateOrderStatus(' + order.id + ', this.value)">';
    for (var j = 0; j < statuses.length; j++) {
      statusSelect += '<option value="' + statuses[j] + '"' + (statuses[j] === order.status ? " selected" : "") + '>' + statusLabel(statuses[j]) + '</option>';
    }
    statusSelect += '</select>';
  }
  return '<article class="order-card"><div><div class="order-head"><h3>Order #' + order.id
    + '</h3><span class="badge">' + statusLabel(order.status) + '</span></div><p>' + items.join(", ")
    + '</p><small>' + escapeHtml(order.customerName) + ' - ' + escapeHtml(order.customerPhone) + ' - '
    + escapeHtml(order.orderDate) + '</small><p class="muted">' + escapeHtml(order.deliveryAddress)
    + '</p></div><div class="order-side"><strong>' + money(order.totalAmount) + '</strong>' + statusSelect + '</div></article>';
}

function statusLabel(status) {
  return String(status || "").split("_").join(" ");
}

function cartTotal() {
  var total = 0;
  for (var i = 0; i < appState.cart.length; i++) {
    total += Number(appState.cart[i].price) * appState.cart[i].quantity;
  }
  return total;
}

function getJson(url, onSuccess, onError) {
  request("GET", url, null, onSuccess, onError);
}

function postForm(url, formData, onSuccess, onError) {
  request("POST", url, formData, onSuccess, onError);
}

function postJson(url, payload, onSuccess, onError) {
  request("POST", url, JSON.stringify(payload), onSuccess, onError, "application/json");
}

function request(method, url, body, onSuccess, onError, contentType) {
  var xhr = new XMLHttpRequest();
  xhr.open(method, url, true);
  xhr.setRequestHeader("Accept", "application/json");
  xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
  if (contentType) {
    xhr.setRequestHeader("Content-Type", contentType);
  }
  xhr.onreadystatechange = function () {
    if (xhr.readyState !== 4) {
      return;
    }
    if (xhr.status >= 200 && xhr.status < 300) {
      var data = null;
      if (xhr.responseText) {
        data = JSON.parse(xhr.responseText);
      }
      if (onSuccess) {
        onSuccess(data);
      }
    } else if (onError) {
      onError(xhr);
    }
  };
  xhr.send(body);
}

function bindClick(id, handler) {
  var element = document.getElementById(id);
  if (element) {
    element.addEventListener("click", handler);
  }
}

function buildFormData(form) {
  var data = new FormData();
  var elements = form.querySelectorAll("input, select, textarea");
  for (var i = 0; i < elements.length; i++) {
    var field = elements[i];
    if (!field.name || field.disabled) {
      continue;
    }
    if ((field.type === "checkbox" || field.type === "radio") && !field.checked) {
      continue;
    }
    data.set(field.name, field.value);
  }
  return data;
}

function validateRequiredFields(form, names) {
  for (var i = 0; i < names.length; i++) {
    var field = getFormField(form, names[i]);
    if (!field) {
      continue;
    }
    if (String(field.value || "").trim() === "") {
      field.focus();
      toast(labelFor(field.name) + " is required.", true);
      return false;
    }
  }
  return true;
}

function getFormField(form, name) {
  return form.querySelector('[name="' + name + '"]');
}

function setFormFieldValue(form, name, value) {
  var field = getFormField(form, name);
  if (field) {
    field.value = value == null ? "" : value;
  }
}

function setFormFieldChecked(form, name, checked) {
  var field = getFormField(form, name);
  if (field) {
    field.checked = Boolean(checked);
  }
}

function labelFor(name) {
  if (name === "restaurantId") {
    return "Restaurant";
  }
  if (name === "deliveryTime") {
    return "Delivery time";
  }
  return name.charAt(0).toUpperCase() + name.slice(1);
}

function bindInput(id, handler) {
  var element = document.getElementById(id);
  if (element) {
    element.addEventListener("input", handler);
    element.addEventListener("change", handler);
  }
}

function toggleElement(id, hidden) {
  var element = document.getElementById(id);
  if (element) {
    element.classList.toggle("hidden", hidden);
  }
}

function elementValue(id) {
  var element = document.getElementById(id);
  return element ? element.value : "";
}

function findById(items, id) {
  for (var i = 0; i < items.length; i++) {
    if (String(items[i].id) === String(id)) {
      return items[i];
    }
  }
  return null;
}

function money(value) {
  return "Rs " + Number(value || 0).toFixed(2);
}

function defaultFoodImage() {
  return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80";
}

function escapeHtml(value) {
  return String(value == null ? "" : value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function emptyRow(cols) {
  return '<tr><td colspan="' + cols + '" class="muted">No records found.</td></tr>';
}

function emptyState(text) {
  return '<div class="empty-state">' + text + '</div>';
}

function setMessage(message, isError) {
  var element = document.getElementById("message");
  if (!element) {
    return;
  }
  element.textContent = message;
  element.classList.toggle("error", Boolean(isError));
}

function toast(message, isError) {
  var element = document.getElementById("toast");
  if (!element) {
    return;
  }
  element.textContent = message;
  element.classList.toggle("error", Boolean(isError));
  element.classList.add("show");
  window.setTimeout(function () {
    element.classList.remove("show");
  }, 2400);
}

function readError(xhr, fallback) {
  if (!xhr || !xhr.responseText) {
    return fallback;
  }
  try {
    var data = JSON.parse(xhr.responseText);
    if (data && data.message) {
      return data.message;
    }
  } catch (e) {
  }
  return xhr.responseText || fallback;
}
