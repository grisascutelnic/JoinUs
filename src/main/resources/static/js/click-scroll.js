//jquery-click-scroll
//by syamsul'isul' Arifin

var sectionArray = [1, 2, 3, 4, 5];

$.each(sectionArray, function(index, value){
     var sectionId = '#' + 'section_' + value;
     var $section = $(sectionId);
     if ($section.length === 0) {
         return;
     }

     $(document).scroll(function(){
         var offsetSection = $section.offset().top - 94;
         var docScroll = $(document).scrollTop();
         var docScroll1 = docScroll + 1;
         
        
         if ( docScroll1 >= offsetSection ){
             var $scrollLinks = $('.navbar-nav .nav-item .nav-link.click-scroll');
             if ($scrollLinks.length === 0) {
                 return;
             }
             $scrollLinks.removeClass('active');
             $scrollLinks.addClass('inactive');
             $scrollLinks.eq(index).addClass('active');
             $scrollLinks.eq(index).removeClass('inactive');
         }
         
     });
    
    $('.click-scroll').eq(index).click(function(e){
        var offsetClick = $section.offset().top - 94;
        e.preventDefault();
        $('html, body').animate({
            'scrollTop':offsetClick
        }, 300)
    });
    
});

$(document).ready(function(){
    var $scrollLinks = $('.navbar-nav .nav-item .nav-link.click-scroll');
    if ($scrollLinks.length === 0) {
        return;
    }
    $scrollLinks.addClass('inactive');
    $scrollLinks.eq(0).addClass('active');
    $scrollLinks.eq(0).removeClass('inactive');
});
