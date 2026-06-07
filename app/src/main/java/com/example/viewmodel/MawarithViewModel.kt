package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.engine.InheritanceEngine
import com.example.engine.InheritanceInput
import com.example.engine.InheritanceResult
import com.example.engine.QuizDatabase
import com.example.engine.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
    StudyGuides,
    Calculator,
    Quiz,
    AiChat
}

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuizState(
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val selectedOptionIndex: Int = -1,
    val isAnswered: Boolean = false,
    val isQuizFinished: Boolean = false,
    val showExplanationDialog: Boolean = false
)

data class UiState(
    val currentTab: AppTab = AppTab.StudyGuides,
    
    // Calculator States
    val calculatorInput: InheritanceInput = InheritanceInput(),
    val calculatorResult: InheritanceResult? = null,
    
    // AI Chat States
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage("ai", "مرحباً بك! أنا مرشدك الذكي لعلم الميراث والمواريث لشهادة البكالوريا الجزائرية. يمكنك أن تطرح علي أي مسألة أو سؤال فقهي أو استفسار عن قانون الأسرة وسأجيبك فوراً!")
    ),
    val aiLoading: Boolean = false,
    val aiError: String? = null,
    val isApiKeyPresent: Boolean = false,

    // Quiz States
    val quizState: QuizState = QuizState()
)

class MawarithViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Evaluate if Gemini Key is available
        val apiKey = BuildConfig.GEMINI_API_KEY
        val present = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "placeholder"
        _uiState.update { it.copy(isApiKeyPresent = present) }
        
        // Compute initial empty calculation
        triggerCalculation()
    }

    fun setTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- Calculator Actions ---

    fun updateCalculatorInput(transform: (InheritanceInput) -> InheritanceInput) {
        _uiState.update { state ->
            val newInput = transform(state.calculatorInput)
            // Ensure mutually exclusive spouses
            var sanitizedInput = newInput
            if (newInput.hasHusband && newInput.hasWife) {
                // If husband was turned on, turn off wife, and vice versa
                if (state.calculatorInput.hasHusband) {
                    sanitizedInput = newInput.copy(hasHusband = false)
                } else {
                    sanitizedInput = newInput.copy(hasWife = false)
                }
            }
            state.copy(calculatorInput = sanitizedInput)
        }
        triggerCalculation()
    }

    private fun triggerCalculation() {
        val input = _uiState.value.calculatorInput
        val result = InheritanceEngine.calculate(input)
        _uiState.update { it.copy(calculatorResult = result) }
    }

    // --- Quiz Actions ---

    fun selectQuizOption(index: Int) {
        if (_uiState.value.quizState.isAnswered) return
        _uiState.update { state ->
            val qState = state.quizState.copy(selectedOptionIndex = index)
            state.copy(quizState = qState)
        }
    }

    fun submitQuizAnswer() {
        val currentQuiz = _uiState.value.quizState
        if (currentQuiz.isAnswered || currentQuiz.selectedOptionIndex == -1) return
        
        val question = QuizDatabase.questions[currentQuiz.currentQuestionIndex]
        val isCorrect = currentQuiz.selectedOptionIndex == question.correctIndex
        val newScore = if (isCorrect) currentQuiz.score + 1 else currentQuiz.score

        _uiState.update { state ->
            val qState = state.quizState.copy(
                isAnswered = true,
                score = newScore,
                showExplanationDialog = true
            )
            state.copy(quizState = qState)
        }
    }

    fun closeExplanation() {
        _uiState.update { state ->
            val qState = state.quizState.copy(showExplanationDialog = false)
            state.copy(quizState = qState)
        }
    }

    fun nextQuizQuestion() {
        val currentQuiz = _uiState.value.quizState
        val nextIndex = currentQuiz.currentQuestionIndex + 1
        if (nextIndex >= QuizDatabase.questions.size) {
            _uiState.update { state ->
                val qState = state.quizState.copy(
                    isQuizFinished = true,
                    showExplanationDialog = false
                )
                state.copy(quizState = qState)
            }
        } else {
            _uiState.update { state ->
                val qState = state.quizState.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOptionIndex = -1,
                    isAnswered = false,
                    showExplanationDialog = false
                )
                state.copy(quizState = qState)
            }
        }
    }

    fun restartQuiz() {
        _uiState.update { state ->
            state.copy(
                quizState = QuizState()
            )
        }
    }

    // --- AI Chat Actions ---

    fun sendChatMessage(prompt: String) {
        if (prompt.trim().isEmpty()) return

        val userMessage = ChatMessage("user", prompt)
        _uiState.update { state ->
            state.copy(
                chatMessages = state.chatMessages + userMessage,
                aiLoading = true,
                aiError = null
            )
        }

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val present = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "placeholder"
                
                val aiReplyText = if (present) {
                    callGeminiApi(prompt)
                } else {
                    simulateLocalAiResponse(prompt)
                }

                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + ChatMessage("ai", aiReplyText),
                        aiLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        aiError = "لم نتمكن من الاتصال بالخادم الذكي: ${e.message}",
                        aiLoading = false
                    )
                }
            }
        }
    }

    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val systemStr = "أنت 'المساعد الذكي لعلم الميراث والمواريث' المصمم لمساعدة طلاب شهادة البكالوريا الجزائرية في مادة العلوم الإسلامية (الشريعة). " +
                    "مهمتك هي الإرشاد الشرعي والقانوني ومساعدة الطلاب في حل المسائل وشرح المفاهيم (أركان، شروط، أسباب، موانع 'عش لك رزق'، طرق بالفرض والتعصيب، المقارنات، الوقف، التنزيل للمواد 169-172). " +
                    "تحدث باللغة العربية الفصحى دائماً بأسلوب تربوي بليغ وبارز."

            val history = _uiState.value.chatMessages.takeLast(10).map { msg ->
                Content(parts = listOf(Part(text = "${if (msg.sender == "user") "أنا الطالب: " else "المرشد الذكي: "}${msg.text}")))
            }

            val request = GenerateContentRequest(
                contents = history + Content(parts = listOf(Part(text = prompt))),
                systemInstruction = Content(parts = listOf(Part(text = systemStr)))
            )

            val service = RetrofitClient.service
            val response = service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "عذراً، لم تنجح الاستجابة الذكية. يرجى إعادة إرسال السؤال."
        } catch (e: Exception) {
            "طلب المساعدة استغرق وقتاً طويلاً: ${e.localizedMessage}. يرجى التحقق من اتصال الإنترنت أو مفتاح API."
        }
    }

    // Fast educational responses for offline/mock environments to sustain UI responsiveness
    private fun simulateLocalAiResponse(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("كلالة") || p.contains("الكلالة") -> {
                "الكلالة لغة: هي الإحاطة؛ ومنه الإكليل لأنه يحيط بالرأس. واصطلاحاً في الشريعة والفقه هي: 'الميت الذي يموت وليس له ولد (فرع وارث مذكر أو مؤنث) ولا والد (أصل مذكر وإن علا)'. وقد ذُكرت في الآية 12 والآية 176 من سورة النساء لتنظيم ميراث الإخوة للأم والإخوة الأشقاء أو لأب."
            }
            p.contains("تنزيل") || p.contains("التنزيل") || p.contains("الوصية الواجبة") -> {
                "نظام التنزيل في قانون الأسرة الجزائري (المواد 169-172) هو إقامة أحفاد المتوفى (أولاد الابن أو أولاد البنت) مقام أصلهم المتوفى في حياة جدهم أو جدتهم. شروطه: ألا يتجاوز ثلث التركة الإجمالية (1/3)، ألا يكون الأحفاد وارثين حقيقيين بجهة أخرى، ويقسم سهم التنزيل على قاعدة للذكر مثل حظ الأنثيين."
            }
            p.contains("عش لك رزق") || p.contains("موانع") || p.contains("الموانع") -> {
                "موانع الإرث السبعة المتفق عليها تُجمع في جملة 'عش لك رزق' وهي:\n" +
                        "1. ع: عدم استهلال الجنين صارخاً (أي ينزل ميتاً).\n" +
                        "2. ش: الشك في أسبقية الوفاة (كالموتى في حادث واحد دون معرفة السابق).\n" +
                        "3. ل: اللعان بين الزوجين.\n" +
                        "4. ك: الكفر (اختلاف الدين).\n" +
                        "5. ر: الرق والعبودية.\n" +
                        "6. ز: الزنى (ولد الزنى لا يرث من الزاني بل من أمه).\n" +
                        "7. ق: القتل العمد للمورث."
            }
            p.contains("الوقف") || p.contains("وقف") -> {
                "الفرق بين الميراث والوقف (سؤال بكالوريا 2024 الجوهري):\n" +
                        "- الميراث: انتقال ملكية إجباري بقوة الشرع والقانون بعد الوفاة، يفتت التركة ويقسمها على الأقارب فقط.\n" +
                        "- الوقف: تبرع اختياري يقوم به الواقف في حياته، يحبس فيه عين المال ويمتنع بيعه أو توارثه مع تسبيل المنفعة لجهات البر والمصالح الخيرية العامة والخاصة."
            }
            p.contains("غراوين") || p.contains("امريتين") || p.contains("الغراوين") -> {
                "المسألتان الغراويتان أو العمريتان هما: (زوج، أم، أب) و (زوجة، أم، أب). سُميتا بذلك لشهرتهما، وقضى فيهما عمر بن الخطاب رضي الله عنه للأم بثلث الباقي لتفادي حرمان الأب من ميزته المعهودة (للذكر مثل حظ الأنثيين) حيث لو أخذت ثلث الكل لزاد نصيبها على الأب."
            }
            else -> {
                "أحسنت القول! لقد استقبلت سؤالك حول: '$prompt'. \n\n" +
                        "بما أن مفتاح API في الوضع الاختياري للنموذج، يجدر التذكير بالقواعد الأساسية لشهادة البكالوريا:\n" +
                        "- أسباب الميراث: النسب الحقيقي، والزوجية الصحيحة.\n" +
                        "- شروط الميراث: تحقق موت المورث حقيقة أو حكماً، تحقق حياة الوارث عند موت المورث، والعلم بجهة الإرث.\n" +
                        "- أقسام الورثة: أصحاب فروض (النصف، الربع، الثمن، الثلثان، الثلث، السدس) وعصبات يحوزون الباقي.\n\n" +
                        "هل تود مني توليد مسألة عملية لحسابها؟"
            }
        }
    }
}
