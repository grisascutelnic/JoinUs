
  (function ($) {
  
  "use strict";

    // MENU
    $('.navbar-collapse a').on('click',function(){
      $(".navbar-collapse").collapse('hide');
    });
    
    // CUSTOM LINK
    $('.smoothscroll').click(function(){
      var el = $(this).attr('href');
      var elWrapped = $(el);
      var header_height = $('.navbar').height();
  
      scrollToDiv(elWrapped,header_height);
      return false;
  
      function scrollToDiv(element,navheight){
        var offset = element.offset();
        var offsetTop = offset.top;
        var totalScroll = offsetTop-navheight;
  
        $('body,html').animate({
        scrollTop: totalScroll
        }, 300);
      }
    });

    function buildAuthUrl(type) {
      var url = new URL(window.location.href);
      var params = new URLSearchParams(url.search);
      params.delete('login');
      params.delete('register');
      var query = params.toString();
      if (type) {
        query = query ? query + '&' + type : type;
      }
      return url.pathname + (query ? '?' + query : '') + url.hash;
    }

    function getAuthTypeFromUrl() {
      var params = new URL(window.location.href).searchParams;
      if (params.has('login')) return 'login';
      if (params.has('register')) return 'register';
      return null;
    }

    function getModal(type) {
      var id = type === 'register' ? 'registerModal' : 'loginModal';
      var el = document.getElementById(id);
      if (!el || !window.bootstrap) return null;
      return bootstrap.Modal.getOrCreateInstance(el);
    }

    function openAuthModal(type, pushState) {
      var modal = getModal(type);
      if (!modal) return;
      if (type === 'login') {
        var registerModal = getModal('register');
        if (registerModal) {
          suppressHistory = true;
          registerModal.hide();
        }
      } else {
        var loginModal = getModal('login');
        if (loginModal) {
          suppressHistory = true;
          loginModal.hide();
        }
      }
      if (pushState) {
        history.pushState({ auth: type }, '', buildAuthUrl(type));
      }
      modal.show();
    }

    function lockScroll() {
      if (document.body.dataset.scrollLock === '1') return;
      document.body.dataset.scrollLock = '1';
      var scrollY = window.scrollY || window.pageYOffset;
      document.body.dataset.scrollY = String(scrollY);
      document.body.style.position = 'fixed';
      document.body.style.top = '-' + scrollY + 'px';
      document.body.style.width = '100%';
    }

    function unlockScroll() {
      if (document.body.dataset.scrollLock !== '1') return;
      var scrollY = parseInt(document.body.dataset.scrollY || '0', 10);
      document.body.style.position = '';
      document.body.style.top = '';
      document.body.style.width = '';
      document.body.dataset.scrollLock = '0';
      window.scrollTo(0, scrollY);
    }

    function closeAuthModals() {
      var loginModal = getModal('login');
      var registerModal = getModal('register');
      if (loginModal) loginModal.hide();
      if (registerModal) registerModal.hide();
    }

    var suppressHistory = false;

    document.addEventListener('click', function (event) {
      var link = event.target.closest('a');
      if (!link) return;
      var trigger = link.getAttribute('data-auth-trigger');
      if (trigger === 'login' || trigger === 'register') {
        event.preventDefault();
        openAuthModal(trigger, true);
        return;
      }
      if (!link.href) return;

      var url = new URL(link.href, window.location.origin);
      if (url.origin !== window.location.origin) return;

      if (url.pathname === '/login') {
        event.preventDefault();
        openAuthModal('login', true);
      }

      if (url.pathname === '/register') {
        event.preventDefault();
        openAuthModal('register', true);
      }
    });

    window.addEventListener('popstate', function () {
      var authType = getAuthTypeFromUrl();
      if (authType) {
        openAuthModal(authType, false);
      } else {
        suppressHistory = true;
        closeAuthModals();
      }
    });

    ['loginModal', 'registerModal'].forEach(function (id) {
      var el = document.getElementById(id);
      if (!el) return;
      el.addEventListener('show.bs.modal', function () {
        lockScroll();
      });
      el.addEventListener('hidden.bs.modal', function () {
        unlockScroll();
        var hasOpenModal = document.querySelector('.auth-modal.show');
        if (suppressHistory) {
          suppressHistory = false;
          if (hasOpenModal) {
            return;
          }
        }
        if (!hasOpenModal) {
          history.replaceState({}, '', buildAuthUrl(null));
        }
      });
    });

    var initialAuth = getAuthTypeFromUrl();
    if (initialAuth) {
      openAuthModal(initialAuth, false);
    }

    var ACTIVITY_TABS = ['info', 'chat', 'announcements'];

    function isValidActivityTab(tab) {
      return ACTIVITY_TABS.indexOf(tab) !== -1;
    }

    function getUrlActivityTab() {
      return new URLSearchParams(window.location.search).get('tab');
    }

    function setPreferredActivityTab(tab) {
      if (!isValidActivityTab(tab)) return;
      window.sessionStorage.setItem('joinusPreferredActivityTab', tab);
    }

    function getPreferredActivityTab() {
      var urlTab = getUrlActivityTab();
      if (isValidActivityTab(urlTab)) {
        return urlTab;
      }

      var stored = window.sessionStorage.getItem('joinusPreferredActivityTab');
      return isValidActivityTab(stored) ? stored : null;
    }

    function syncPreferredActivityTabContext() {
      // Activities listing should always open details on Info tab.
      if (window.location.pathname === '/activities') {
        setPreferredActivityTab('info');
        return;
      }

      var tab = getUrlActivityTab();
      if (isValidActivityTab(tab)) {
        setPreferredActivityTab(tab);
      }
    }

    function applyPreferredActivityTabContext() {
      var preferredTab = getPreferredActivityTab();
      if (!preferredTab) {
        return;
      }

      var activitiesNavLink = document.querySelector('.navbar-nav .nav-link[href="/activities"]');
      if (activitiesNavLink) {
        activitiesNavLink.setAttribute('href', '/activities');
        activitiesNavLink.addEventListener('click', function () {
          setPreferredActivityTab('info');
        });
      }
    }

    var navChatLink = document.querySelector('.navbar-nav .nav-link[href="/chat"]');
    if (navChatLink) {
      navChatLink.addEventListener('click', function () {
        setPreferredActivityTab('chat');
      });
    }

    syncPreferredActivityTabContext();
    applyPreferredActivityTabContext();

    function ensureNotificationsPanel() {
      var existing = document.getElementById('notificationsPanel');
      if (existing) {
        return existing;
      }

      var panel = document.createElement('div');
      panel.className = 'offcanvas offcanvas-end';
      panel.setAttribute('data-bs-scroll', 'true');
      panel.setAttribute('tabindex', '-1');
      panel.id = 'notificationsPanel';
      panel.setAttribute('aria-labelledby', 'notificationsPanelLabel');
      panel.innerHTML =
        '<div class="offcanvas-header">' +
          '<h5 class="offcanvas-title text-white" id="notificationsPanelLabel">Notificari</h5>' +
          '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="offcanvas" aria-label="Close"></button>' +
        '</div>' +
        '<div class="offcanvas-body d-flex flex-column"></div>';
      document.body.appendChild(panel);
      return panel;
    }

    function escapeHtml(value) {
      var safe = value == null ? '' : String(value);
      return safe
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    function getCsrfToken() {
      var csrfInput = document.querySelector('input[name="_csrf"]');
      return csrfInput ? csrfInput.value : null;
    }

    function formatRelativeTime(isoDate) {
      if (!isoDate) return 'Acum';
      var date = new Date(isoDate);
      if (Number.isNaN(date.getTime())) return 'Acum';

      var diffMs = Date.now() - date.getTime();
      var minutes = Math.max(1, Math.floor(diffMs / 60000));
      if (minutes < 60) return 'Acum ' + minutes + ' min';

      var hours = Math.floor(minutes / 60);
      if (hours < 24) return 'Acum ' + hours + ' h';

      var days = Math.floor(hours / 24);
      return 'Acum ' + days + ' zile';
    }

    function updateNotificationBadge(unreadCount) {
      var badge = document.getElementById('navbarNotificationBadge');
      if (!badge) return;

      if (!unreadCount || unreadCount < 1) {
        badge.classList.add('d-none');
        badge.textContent = '0';
        return;
      }

      badge.classList.remove('d-none');
      badge.textContent = unreadCount > 99 ? '99+' : String(unreadCount);
    }

    function renderNotifications(data) {
      var panel = ensureNotificationsPanel();
      var body = panel.querySelector('.offcanvas-body');
      if (!body) return;

      var notifications = Array.isArray(data && data.notifications) ? data.notifications : [];
      updateNotificationBadge((data && data.unreadCount) || 0);

      if (notifications.length === 0) {
        body.innerHTML =
          '<div class="notification-empty mb-3">Nu ai notificari momentan.</div>' +
          '<div class="mt-auto"><a href="#" class="btn custom-btn custom-border-btn w-100" data-bs-dismiss="offcanvas">Inchide</a></div>';
        return;
      }

      var itemsHtml = notifications.map(function (item) {
        var readClass = item.read ? ' notification-item--read' : '';
        var title = escapeHtml(item.title || 'Notificare');
        var message = escapeHtml(item.message || '');
        var relative = formatRelativeTime(item.createdAt);

        return (
          '<div class="notification-item' + readClass + '">' +
            '<strong class="text-white">' + title + '</strong>' +
            '<p class="mb-1">' + message + '</p>' +
            '<small>' + escapeHtml(relative) + '</small>' +
          '</div>'
        );
      }).join('');

      body.innerHTML =
        '<div class="notifications-list">' + itemsHtml + '</div>' +
        '<div class="mt-auto pt-2">' +
          '<a href="#" class="btn custom-btn custom-border-btn w-100" data-bs-dismiss="offcanvas">Inchide</a>' +
        '</div>';
    }

    function patchNotification(url) {
      var headers = {
        'X-Requested-With': 'XMLHttpRequest'
      };
      var csrfToken = getCsrfToken();
      if (csrfToken) {
        headers['X-CSRF-TOKEN'] = csrfToken;
      }

      return fetch(url, {
        method: 'PATCH',
        headers: headers
      });
    }

    function loadNotifications() {
      var trigger = document.querySelector('[data-notifications-trigger="true"]');
      if (!trigger) return;

      ensureNotificationsPanel();
      fetch('/api/notifications?limit=8', {
        headers: {
          'Accept': 'application/json'
        }
      })
        .then(function (response) {
          if (!response.ok) throw new Error('Notifications fetch failed');
          return response.json();
        })
        .then(renderNotifications)
        .catch(function () {
          updateNotificationBadge(0);
        });
    }

    var notificationPanel = ensureNotificationsPanel();
    if (notificationPanel && !notificationPanel.dataset.notificationsBound) {
      notificationPanel.dataset.notificationsBound = 'true';

      ['copy', 'cut', 'selectstart'].forEach(function (eventName) {
        notificationPanel.addEventListener(eventName, function (event) {
          if (event.target && event.target.closest('.notifications-list')) {
            event.preventDefault();
          }
        });
      });

      notificationPanel.addEventListener('show.bs.offcanvas', function () {
        updateNotificationBadge(0);
        patchNotification('/api/notifications/read-all')
          .finally(loadNotifications);
      });
    }

    if (document.querySelector('[data-notifications-trigger="true"]')) {
      loadNotifications();
      setInterval(loadNotifications, 30000);
    }

    function initBirthDateSelects() {
      var daySelect = document.getElementById('birthDaySelect');
      var monthSelect = document.getElementById('birthMonthSelect');
      var yearSelect = document.getElementById('birthYearSelect');
      var hiddenInput = document.getElementById('birthDateInput');
      if (!daySelect || !monthSelect || !yearSelect || !hiddenInput) return;

      function addOption(select, value, text) {
        var option = document.createElement('option');
        option.value = value;
        option.textContent = text;
        select.appendChild(option);
      }

      daySelect.innerHTML = '';
      monthSelect.innerHTML = '';
      yearSelect.innerHTML = '';

      addOption(daySelect, '', 'Zi');
      addOption(monthSelect, '', 'Luna');
      addOption(yearSelect, '', 'An');

      for (var day = 1; day <= 31; day++) {
        addOption(daySelect, String(day).padStart(2, '0'), String(day));
      }
      for (var month = 1; month <= 12; month++) {
        addOption(monthSelect, String(month).padStart(2, '0'), String(month));
      }

      var currentYear = new Date().getFullYear();
      var maxYear = currentYear - 12;
      var minYear = 1900;
      for (var year = maxYear; year >= minYear; year--) {
        addOption(yearSelect, String(year), String(year));
      }

      function syncHidden() {
        var day = daySelect.value;
        var month = monthSelect.value;
        var year = yearSelect.value;
        if (day && month && year) {
          hiddenInput.value = year + '-' + month + '-' + day;
        } else {
          hiddenInput.value = '';
        }
      }

      daySelect.addEventListener('change', syncHidden);
      monthSelect.addEventListener('change', syncHidden);
      yearSelect.addEventListener('change', syncHidden);
      syncHidden();
    }

    initBirthDateSelects();
  
  })(window.jQuery);
