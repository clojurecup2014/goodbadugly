var Repos = {
    load: function() {
        $.get("/api/repos")
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
    error: function() {

    }
};

$(function() {
    Repos.load();
});
