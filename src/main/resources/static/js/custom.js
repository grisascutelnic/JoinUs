
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
  
  })(window.jQuery);
