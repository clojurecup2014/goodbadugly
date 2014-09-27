var Repos = {
    load: function() {
        $.get("/api/repos")
            .done(Repos.process)
            .fail(Repos.error);
    },
    process:function(data) {

    },
    error: function() {

    }
};

$(function() {
    Repos.load();
});
