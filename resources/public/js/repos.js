var Repos = {
    onUrl: "/api/on",
    offUrl: "/api/off",
    reposUrl: "/api/repos",
    init: function() {
        Repos.load();
        $("#repos").on("click", "button", Repos.toggle);
    },
    load: function() {
        $.get(Repos.reposUrl)
            .done(Repos.process)
            .fail(Repos.error);
    },
    process:function(data) {
        var items = $.parseJSON(data);
        var listSrc = $("#repo-list-tplt").html();
        var listTplt = Handlebars.compile(listSrc);
        var itemSrc = $("#repo-item-tplt").html();
        var itemTplt = Handlebars.compile(itemSrc);

        var itemsHTML = "";
        $.each(items, function(index, item) {
            itemsHTML += itemTplt(item);
        });

        var html = listTplt({items : itemsHTML});
        $("#repos").html(html);

        var options = {
            valueNames: [ 'name' ]
        };
        var userList = new List('repo-list', options);
    },
    error: function(err) {
        alert("Oops, seems like we messed up! :(");
    },
    toggle: function() {
        var btn = $(this);
        var url = Repos.isOn(btn)? Repos.offUrl : Repos.onUrl;
        var fullname = btn.attr("data-full-name");
        var parts = fullname.split("/");
        var user = parts[0];
        var repo = parts[1];
        var data = {user: user, repo: repo};
        var toggle = function () {
            Repos.updateButton(btn);
        };
        $.get(Repos.onUrl, data)
            .done(toggle)
            .fail(Repos.error);
    },
    updateButton: function(btn) {
        if(Repos.isOn(btn))
            btn.removeClass('on').addClass('off');
        else
            btn.removeClass('of').addClass('on');
    },
    isOn: function(btn) {
        btn.hasClass('on');
    }
};

$(function() {
    Repos.init();
});
