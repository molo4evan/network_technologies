import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.Exception
import java.net.URLEncoder

object Resolver {
    enum class Language(val abbr: String, val title: String) {
        AZ("az", "азербайджанский"),
        EN("en", "английский"),
        ARAB("ar", "арабский"),
        ARM("hy", "армянский"),
        BE("be", "белорусский"),
        BG("bg", "болгарский"),
        VEN("hu", "венгерский"),
        NL("nl", "голландский"),
        GRE("el", "греческий"),
        GRU("ka", "грузинский"),
        DA("da", "датский"),
        IVR("he", "иврит"),
        ID("yi", "идиш"),
        IT("it", "итальянский"),
        ES("es", "испанский"),
        KZ("kk", "казахский"),
        KY("ky", "киргизский"),
        CH("zh", "китайский"),
        KO("ko", "корейский"),
        LA("la", "латынь"),
        LV("lv", "латышский"),
        LT("lt", "литовский"),
        MN("mn", "монгольский"),
        DE("de", "немецкий"),
        NO("no", "норвежский"),
        PL("pl", "польский"),
        RU("ru", "русский"),
        SR("sr", "сербский"),
        TT("tt", "татарский"),
        TR("tr", "турецкий"),
        UK("uk", "украинский"),
        FI("fi", "финский"),
        FR("fr", "французский"),
        CS("cs", "чешский"),
        SV("sv", "шведский"),
        EST("et", "эстонский"),
        ESP("eo", "эсперанто"),
        JA("ja", "японский");

        companion object {
            fun getLanguage(abbr: String): Language? {
                for (lang in Language.values()){
                    if (lang.abbr == abbr){
                        return lang
                    }
                }
                return null
            }
        }
    }

    private const val KEY_API = "trnsl.1.1.20181106T170849Z.ca713218aac5955b.b94d9def9d74291d65ce9a4ff8ba1b8b7da85b27"
    private const val URL = "https://translate.yandex.net/api/v1.5/tr.json/detect?key=$KEY_API&text="
    private val client = OkHttpClient()
    private val gson = Gson()

    fun resolve(text: String): String {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val query = URL + encodedText

        val request = Request.Builder().url(query).get().build()

        val response = client.newCall(request).execute()

        val answer = try {
            gson.fromJson(response.body()?.string() ?: throw JsonSyntaxException("null"), YandexAnswer::class.java)
        } catch (ex: JsonSyntaxException) {
            throw Exception("can't parse Yandex response")
        }

        if (answer.code != "200") {
            throw Exception("Bad status of resolving")
        }

        val language = Language.getLanguage(answer.lang)

        return language?.title ?: answer.lang
    }
}