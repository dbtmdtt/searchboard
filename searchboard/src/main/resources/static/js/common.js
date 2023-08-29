const urlParams = new URLSearchParams(window.location.search);

document.addEventListener("DOMContentLoaded", init());
async function init(){
    await setLnbHref();
    await setMoreHref()
    const keyword = keywordInput.val();
    if (keyword.trim() === '') {
        return;
    }
    function fetchAutoRecommendations() {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", "/topWord", true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    var autoRecommendList = JSON.parse(xhr.responseText);
                    displayAutoRecommendations(autoRecommendList);
                } else {
                    console.error("자동 추천을 불러오는 데 실패했습니다.");
                }
            }
        };
        xhr.send();
    }

    function displayAutoRecommendations(autoRecommendList) {
        var autoRecommendElement = document.getElementById("topWord");
        autoRecommendElement.innerHTML = ""; // 기존 내용 초기화

        var ul = document.createElement("ul");
        ul.setAttribute("id", "autoRecommendList");

        for (var i = 0; i < autoRecommendList.length; i++) {
            var listItem = document.createElement("li");
            listItem.textContent = autoRecommendList[i];
            ul.appendChild(listItem);
        }

        autoRecommendElement.appendChild(ul);
    }

    fetchAutoRecommendations();

    $.ajax({
        url: '/forbiddenWord',
        type: 'GET',
        success: function (forbiddenWords) {
            if (forbiddenWords.includes(keyword)) {
                // Show alert for forbidden word
                console.log('금칙어');
                alert('금칙어가 포함되어 있습니다.');

            } else {
                // Proceed with search or other actions
                // searchForm.unbind('submit').submit();
            }
        },
        error: function (error) {
            // console.error('Error:', error);
        }
    });
    $.ajax({
        type: "GET",
        url: "/getTypoSuggest", // 오타 교정 컨트롤러의 URL에 맞게 수정
        data: {
            search: keyword
        },
        success: function (response) {
            if (response) {
                // 검색어 확인 메시지 표시
                var searchResultsDiv = $("#searchResults");
                searchResultsDiv.html("<p>오타 교정된 단어: " + response + "</p><button id='confirmButton'>이 단어로 검색</button>");
                searchResultsDiv.css("display", "block");
                searchResultsDiv.show;

                // 새 이벤트 추가
                $("#confirmButton").off("click").on("click", function (e) {
                    e.preventDefault();
                    // 검색어 변경 후 다시 검색 폼 제출
                    $("#keyword").val(response);

                    // 검색어 변경 후 서버로 검색 요청
                    $("#searchForm").submit();
                });
            } else {
                $("#searchResults").html("<p>검색 결과가 없습니다.</p>");
            }
        },
        error: function () {
            $("#searchResults").html("<p>검색 중 오류가 발생했습니다.</p>");
        }
    });

    $.ajax({
        type: "GET",
        url: "/recommend",
        data: {
            keyword: keyword
        },
        success: function (response) {
            if (response && response.length > 0) {
                var recommendDiv = $("#recommend");
                recommendDiv.empty(); // 기존 내용 비우기

                // 추천 단어 리스트 출력
                var ul = $("<ul>");
                $.each(response, function (index, word) {
                    var li = $("<li >").text(word);
                    ul.append(li);
                });
                recommendDiv.append(ul);
            } else {
                $("#recommend").html("<p>수동 추천 단어가 없습니다.</p>");
            }
        },
        error: function () {
            $("#recommend").html("<p>추천 단어를 불러오는 중 오류가 발생했습니다.</p>");
        }
    });

    $.ajax({
        type: "GET",
        url: "/autoRecommend",
        data: {
            keyword: keyword
        },
        success: function (response) {
            if (response && response.length > 0) {
                var recommendDiv = $("#autoRecommend");
                recommendDiv.empty(); // 기존 내용 비우기

                // 추천 단어 리스트 출력
                var ul = $("<ul>");
                $.each(response, function (index, word) {
                    var li = $("<li >").text(word);
                    ul.append(li);
                });
                recommendDiv.append(ul);
            } else {
                $("#autoRecommend").html("<p>자동 추천 단어가 없습니다.</p>");
            }
        },
        error: function () {
            $("#autoRecommend").html("<p>추천 단어를 불러오는 중 오류가 발생했습니다.</p>");
        }
    });

}
function setLnbHref() {
    let originParams = new URLSearchParams("?" + urlParams.toString());
    originParams.set("page", 1);
    document.querySelectorAll("ul.lnb > li > a").forEach((tab) => {
        // var url = tab.id === "integration" ? "/" : tab.id;
        var url = tab.id === "integration" ? "integration" : tab.id;
        originParams.set("category", url);
        // tab.setAttribute("href", url + "?" + originParams.toString());
        tab.setAttribute("href",  "?" + originParams.toString() );
    });
}
function setMoreHref() {
    let originParams = new URLSearchParams("?" + urlParams.toString());
    originParams.set("page", 1);
    document.querySelectorAll("p.more > a").forEach((more) => {
        var url = more.parentElement.parentElement.id + "?";
        more.setAttribute("href", url + originParams.toString());
    });
}


//자동 완성
const autocompleteResults = $('#autocompleteResults');

const keywordInput = $('#keyword');
keywordInput.on("input", updateForm);
autocompleteResults.on("click", ".clickAutocomplete", autoComplete);
function updateForm(e){
    const keyword = keywordInput.val();
    console.log("input"+keyword);
    if (keyword.trim() === '') {
        autocompleteResults.hide(); // Use jQuery's hide() function
        return;
    }

    $.ajax({
        url: '/search',
        method: 'GET',
        data: { keyword: keyword },
        success: function(data) {
            autocompleteResults.empty();
            if (data) {
                const resultsList = $('<ul></ul>');
                console.log("asdf");
                data.forEach(function(keyword) {
                    const listItem = $('<li class = "clickAutocomplete" style="list-style-type: none;  cursor: pointer;">' + keyword + '</li>');
                    listItem.appendTo(resultsList);
                });

                // Clear previous results and append new ones
                autocompleteResults.empty().append(resultsList);
                autocompleteResults.show(); // Use jQuery's show() function
            }
        },
        error: function(error) {
            console.error('Error:', error);
        }
    });
}
function goToRecovery(){
    console.log("click")
    const newUrl = 'http://localhost:8080/';

// 쿼리 매개변수를 추가하거나 수정합니다.
// 예를 들어, reSearch 매개변수를 삭제하려면:
// newUrl += '?searchCategory=all&category=%EC%A0%95%EC%B9%98&sortOrder=dateDesc&page=3';
// 혹은, 모든 쿼리 매개변수를 제거하려면:
// newUrl += '?';

// 변경된 URL로 리디렉션합니다.
    window.location.href = newUrl;
}
function autoComplete(e){
    const clickedKeyword = $(e.target).text();
    keywordInput.val(clickedKeyword);
    autocompleteResults.hide();
}
let currentSortOrder;
function sort1(e) {

    const currentUrl = new URL(window.location.href);
    const queryParams = new URLSearchParams(currentUrl.search);
    const sortOrder = queryParams.get("sortOrder");

    if (sortOrder === "dateDesc" || sortOrder === "dateAsc" || sortOrder === null || sortOrder === "accuracyDesc" || sortOrder === "accuracyAsc" ) {
        // 현재 정렬 방식에 따라 반대로 변경
        currentSortOrder = sortOrder === "dateDesc" ? "dateAsc" : "dateDesc";
    }

    // URL 업데이트
    const updatedUrl = updateQueryStringParameter(window.location.href, "sortOrder", currentSortOrder);
    window.location.href = updatedUrl;

}
function sort2(e) {
    const currentUrl = new URL(window.location.href);
    const queryParams = new URLSearchParams(currentUrl.search);


    const sortOrder = queryParams.get("sortOrder");
    if (sortOrder === "accuracyDesc" || sortOrder === "accuracyAsc" || sortOrder == null || sortOrder === "dateDesc" || sortOrder === "dateAsc") {
        currentSortOrder = sortOrder === "accuracyDesc" ? "accuracyAsc" : "accuracyDesc";
    }else{
        currentSortOrder = "accuracyDesc"
    }

    // URL 업데이트
    const updatedUrl = updateQueryStringParameter(window.location.href, "sortOrder", currentSortOrder);
    window.location.href = updatedUrl;

}


//조회기간
function showRadioButton(){
    var radioDiv = $("#radioDiv");
    radioDiv.css("display","block");
}

const directInputRadio = document.getElementById("directInput");
const dateDiv = document.getElementById("date");

directInputRadio.addEventListener("click", function() {
    dateDiv.style.display = directInputRadio.checked ? "flex" : "none";
});
function searchPeriod(){
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const directInputRadio = document.getElementById("directInput");

    let periodStart, periodEnd;
    const selectedRadio = document.querySelector('input[name="chk_info"]:checked');
    if (selectedRadio && selectedRadio.value === "0") {
        // "전체" 옵션을 선택한 경우
        const currentDate = new Date();
        periodStart = "2000-01-01";
        periodEnd = currentDate.toISOString().split('T')[0];
    } else if (directInputRadio.checked) {
        periodStart = startDate;
        periodEnd = endDate;
    } else {
        const selectedRadio = document.querySelector('input[name="chk_info"]:checked');
        if (selectedRadio) {
            const days = parseInt(selectedRadio.value);
            const currentDate = new Date();

            periodEnd = currentDate.toISOString().split('T')[0];
            const startDateObj = new Date(currentDate);
            startDateObj.setDate(currentDate.getDate() - days);
            periodStart = startDateObj.toISOString().split('T')[0];
        }
    }

    // URL 업데이트
    const updatedUrl = updateQueryStringParameter(window.location.href, "periodStart", periodStart);
    const finalUrl = updateQueryStringParameter(updatedUrl, "periodEnd", periodEnd);
    console.log(finalUrl);
    window.location.href = finalUrl;
}
///////////////////////////////////////////////////////////
function goToPage(pageNumber) {
    var currentUrl = window.location.href;
    var newUrl = updateQueryStringParameter(currentUrl, 'page', pageNumber);
    console.log(newUrl);
    window.location.href = newUrl;
}
// 선택된 카테고리 가져오는 함수
function getSelectedCategories() {
    const searchCategories = document.getElementsByName("searchCategory");
    const selectedCategories = Array.from(searchCategories)
        .filter(checkbox => checkbox.checked)
        .map(checkbox => checkbox.value);

    return selectedCategories;
}


const keyword = urlParams.get("keyword");
const reSearch = urlParams.has("reSearch");
const searchCategories = urlParams.getAll("searchCategory");

document.getElementById("keyword").value = keyword;
document.getElementById("reSearch").checked = reSearch;
searchCategories.forEach(category => {
    const checkbox = document.querySelector(`input[name="searchCategory"][value="${category}"]`);
    if (checkbox) {
        checkbox.checked = true;
    }
});
function updateQueryStringParameter(uri, key, value) {
    const re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    const separator = uri.indexOf("?") !== -1 ? "&" : "?";
    if (uri.match(re)) {
        return uri.replace(re, "$1" + key + "=" + value + "$2");
    } else {
        return uri + separator + key + "=" + value;
    }
}
// "전체검색" 체크박스 클릭 시 다른 체크박스 상태 변경
document.getElementById('selectAll').addEventListener('click', function () {
    var checkboxes = document.querySelectorAll('input[name="searchCategory"]:not(#selectAll)');
    for (var i = 0; i < checkboxes.length; i++) {
        checkboxes[i].checked = false;
    }
});

// 다른 체크박스 클릭 시 "전체검색" 체크박스 상태 변경
var otherCheckboxes = document.querySelectorAll('input[name="searchCategory"]:not(#selectAll)');
for (var i = 0; i < otherCheckboxes.length; i++) {
    otherCheckboxes[i].addEventListener('click', function () {
        document.getElementById('selectAll').checked = false;
    });
}






const searchForm = $('#searchForm');

function search(event) {
    event.preventDefault();
    const checkReSearch = $('#reSearch').prop('checked');


    console.log(checkReSearch);
    var currentUrl = new URL(window.location.href);
    console.log(currentUrl);
    var keyword = document.getElementById("keyword").value;
    if (checkReSearch) {
        const preKeyword = currentUrl.searchParams.get("reKeyword");

        if (preKeyword !== null) {
            currentUrl.searchParams.set("keyword", keyword)
            var keyword = preKeyword + "," + keyword;
            currentUrl.searchParams.set("reKeyword", keyword);

        }else{
            currentUrl.searchParams.set("reKeyword", keyword);
            currentUrl.searchParams.set("keyword", keyword)

        }
    } else {
        const preKeyword = currentUrl.searchParams.get("reKeyword");
        currentUrl.searchParams.set("keyword", keyword)
        if (preKeyword !== null) {
            currentUrl.searchParams.delete("reKeyword");
        }
    }

    const selectedCategories = Array.from(document.querySelectorAll('input[name="searchCategory"]:checked')).map(checkbox => checkbox.value);
    currentUrl.searchParams.delete("searchCategory");
    selectedCategories.forEach(category => {
        currentUrl.searchParams.append("searchCategory", category);
    });
    console.log("url", currentUrl);
    window.location.href = currentUrl;


}
document.getElementById("submitBtn").addEventListener("click", function () {
    // Get input values
    var phraseSearchValue = document.getElementById("phraseSearch").value;
    var wordSearchValue = document.getElementById("wordSearch").value;
    var notFoundWordValue = document.getElementById("notFoundWord").value;
    // Split input values into arrays
    var phraseSearchArray = phraseSearchValue.split(",");
    var wordSearchArray = wordSearchValue.split(",");
    var notFoundWordArray = notFoundWordValue.split(",");

    // Create SearchParseDto object
    var searchParseDto = {
        matchPhrase: phraseSearchArray,
        mustNot: notFoundWordArray,
        must: wordSearchArray
    };
    fetch('/', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(searchParseDto),
    }).then(response => {

    }).catch(error => {
        // 에러 처리
    });

    console.log(searchParseDto);
});


