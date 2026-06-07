package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.InheritanceInput
import com.example.engine.QuizDatabase
import com.example.viewmodel.AppTab
import com.example.viewmodel.MawarithViewModel
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MawarithDashboard(viewModel: MawarithViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Force RTL layout direction for pristine Arabic language screens
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الميراث الذكي",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.StudyGuides,
                        onClick = { viewModel.setTab(AppTab.StudyGuides) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "الدروس وملخص المنهج") },
                        label = { Text("الملخصات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.Calculator,
                        onClick = { viewModel.setTab(AppTab.Calculator) },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = "حساب الميراث") },
                        label = { Text("الحاسبة", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("calculator_tab_button")
                    )
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.Quiz,
                        onClick = { viewModel.setTab(AppTab.Quiz) },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "اختبارات تفاعلية") },
                        label = { Text("اختبر نفسك", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.AiChat,
                        onClick = { viewModel.setTab(AppTab.AiChat) },
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = "دردشة ذكية") },
                        label = { Text("مساعد ذكي", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.StudyGuides -> StudyGuidesScreen()
                        AppTab.Calculator -> CalculatorScreen(uiState, viewModel)
                        AppTab.Quiz -> QuizScreen(uiState, viewModel)
                        AppTab.AiChat -> AiChatScreen(uiState, viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. STUDY GUIDES TAB (الملخصات والدروس)
// ==========================================

@Composable
fun StudyGuidesScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "الدليل الشامل لمراجعة الميراث والفرائض",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "منهاج رسمي متوافق 100% مع البكالوريا الجزائرية وقانون الأسرة.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            StudyCategoryCard(
                title = "المبحث الأول: التأصيل المفاهيمي وتاريخ الميراث",
                icon = Icons.Default.MenuBook,
                content = "• الميراث لغة: مشتق من التراث (وراث) ويعني البقاء والانتقال.\n" +
                        "• اصطلاحاً: حق مالي قابل للتجزئة والثبوت لمستحقه بعد وفاة مالكه.\n" +
                        "• في الجاهلية: كان يعتمد على القوة البدنية والذكورة وحمل السلاح وحرمان الضعفة والنساء والولدان.\n" +
                        "• في الإسلام: قرر الإسلام نظاماً عادلاً لتوريث الفئات الأقرب رداً على ظلم الجاهلية وجعلها فريضة ربانية مقتسمة."
            )
        }

        item {
            StudyCategoryCard(
                title = "المبحث الثاني: شروط، أركان، وأسباب الميراث",
                icon = Icons.Default.CheckCircle,
                content = "• أركان الميراث ثلاثة:\n" +
                        "  1. المورّث (الميت حقيقة أو حكماً كالمفقود).\n" +
                        "  2. الوارث (الحي بعد موت المورث حقيقة أو حكماً كالحمل المستهل).\n" +
                        "  3. الموروث (التركة أو الأموال المخلفة).\n" +
                        "• شروط الإرث ثلاثة:\n" +
                        "  1. تحقق موت المورث.\n" +
                        "  2. تحقق حياة الوارث وقت موت المورث.\n" +
                        "  3. العلم بالجهة المقتضية للإرث.\n" +
                        "• أسباب الميراث اثنان:\n" +
                        "  1. النسب الحقيقي (القرابة الدموية).\n" +
                        "  2. الزوجية الصحيحة (عقد الزواج القائم شرعاً)."
            )
        }

        item {
            StudyCategoryCard(
                title = "المبحث الثالث: موانع الإرث السبعة (عش لك رزق)",
                icon = Icons.Default.Block,
                content = "تُجمع موانع الإرث في قانون الأسرة الجزائري والشريعة في عبارة 'عش لك رزق' وهي:\n" +
                        "• ع (عدم الاستهلال): نزول الجنين ميتاً دون علامة حياة كالصراخ.\n" +
                        "• ش (الشك في أسبقية الوفاة): الموت في حادث مشترك كالحريق دون معرفة من مات أولاً.\n" +
                        "• ل (اللعان): تهمة الزوج لزوجته بالزنا مما ينفي نسب الميراث.\n" +
                        "• ك (الكفر): اختلاف الملة والدين فلا يرث المسلم الكافر والعكس.\n" +
                        "• ر (الرق): العبودية والرق ليس لهما حق الملك.\n" +
                        "• ز (الزنى): ولد الزنا لا يرث من والده البيولوجي بل يتوارث مع أمه فقط.\n" +
                        "• ق (القتل العمد): قتل الوارث لمورثه يحرمه من الإرث بنقيض قصده كعقوبة فقهية وقانونية."
            )
        }

        item {
            StudyCategoryCard(
                title = "المبحث الرابع: طرق الإرث (الفرض مقابل التعصيب)",
                icon = Icons.Default.SwapHoriz,
                content = "• الميراث بالفرض:\n" +
                        "  هو أخذ حصة معلومة بكسر شرعي مقدر لا زيادة ولا نقصان فيه إلا بعول أو رد.\n" +
                        "  الفروض الستة المقدرة: النصف (1/2)، الربع (1/4)، الثمن (1/8)، الثلثان (2/3)، الثلث (1/3)، السدس (1/6).\n\n" +
                        "• الميراث بالتعصيب:\n" +
                        "  هو أخذ نصيب غير محدد وهو حيازة التركة كاملة عند الانفراد أو ما بقي بعد عزل نصيب ذوي الفروض.\n" +
                        "  أنواعه: عصبة بالنفس (الذكور كالأبناء والآباء)، وعصبة بالغير (الإناث مع الذكور المساوين كالابن مع البنت للذكر مثل حظ الأنثيين)، وعصبة مع الغير (كالأخوات مع البنات).\n\n" +
                        "• قواعد الحجب:\n" +
                        "  يُحجب الجد بالأب، وتُحجب الجدات بالأم، ويُحجب ابن الابن بالابن والأشقاء بالأب والفرع الوارث المذكر."
            )
        }

        item {
            StudyCategoryCard(
                title = "المقايسة بين الميراث والوقف (شهادة بكالوريا 2024)",
                icon = Icons.Default.SwapHoriz,
                content = "• الميراث: انتقال ملكية إجباري يثبت للمستحق بعد الوفاة بقوة الدين والقانون، يهدف لتفتيت الثروة ومنع تراكمها في يد واحدة مع رعاية الأسرة.\n" +
                        "• الوقف: تبرع مالي اختياري بنية صالحة في حياة الواقف بحبس العين واستثمار ريعها وإخراج منفعتها لمؤسسات الخير العامة كدار الأيتام والفقراء.\n" +
                        "• الأثر الاجتماعي: الميراث يحمي القرابة الحقيقية والزوجة، بينما الوقف ينشئ تكافلاً مجتمعياً شاملاً لا ينقطع للأجيال."
            )
        }

        item {
            StudyCategoryCard(
                title = "المبحث الخامس: نظام التنزيل (المواد 169-172 من قانون الأسرة)",
                icon = Icons.Default.Gavel,
                content = "• تعريفه: وصية واجبة يفرضها القانون لتنزيل الأحفاد الأيتام (أولاد الابن أو البنت الميتين في حياة الجد) منزلة والدهم كأنه حي وقت الوفاة.\n" +
                        "• الحكمة منها: حماية الأيتام من حجب الأعمام الكلي وتفادي ضياعهم المالي.\n" +
                        "• الشروط القانونية:\n" +
                        "  1. ألا يكونوا وارثين حقيقيين من الميراث بوجود مصل خفي.\n" +
                        "  2. ألا يتخطى مقدار التنزيل ثلث التركة الإجمالية (1/3)، وإذا تخطاها رُدّ للثلث.\n" +
                        "  3. ألا يكون المورث قد أعطاهم سلفاً تبرعاً هبة تماثل حقهم.\n" +
                        "  4. يُقتسم سهم التنزيل للذكر مثل حظ الأنثيين."
            )
        }
    }
}

@Composable
fun StudyCategoryCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(animationSpec = spring()),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        textDirection = TextDirection.Rtl
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// 2. LIVE CALCULATOR TAB (حاسبة الميراث)
// ==========================================

@Composable
fun CalculatorScreen(uiState: UiState, viewModel: MawarithViewModel) {
    val result = uiState.calculatorResult
    val input = uiState.calculatorInput

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "حاسبة التركات والفرائض التفاعلية",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "أدخل بيانات الورثة الحقيقيين المتوفى عنهم لتحسب أنصبتهم الحقيقية وفق قواعد البكالوريا وتفصيل العول والتنزيل.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Spouses (الورثة من الزوجية)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الspouse (عقد الزوجية القائم)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("هل ترك زوجاً؟", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = input.hasHusband,
                            onCheckedChange = { value ->
                                viewModel.updateCalculatorInput { it.copy(hasHusband = value) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("husband_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("هل ترك زوجة/زوجات؟", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = input.hasWife,
                            onCheckedChange = { value ->
                                viewModel.updateCalculatorInput { it.copy(hasWife = value) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("wife_switch")
                        )
                    }

                    if (input.hasWife) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عدد الزوجات (المشاركات في الربع أو الثمن)", style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (input.wivesCount > 1) {
                                            viewModel.updateCalculatorInput { it.copy(wivesCount = input.wivesCount - 1) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "نقص")
                                }
                                Text("${input.wivesCount}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("wives_count_text"))
                                IconButton(
                                    onClick = {
                                        if (input.wivesCount < 4) {
                                            viewModel.updateCalculatorInput { it.copy(wivesCount = input.wivesCount + 1) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Lineage / Parents & Siblings (الأصول والحواشي)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الأصول (الأبوان/الوالدان والعدد)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الأب على قيد الحياة", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = input.fatherAlive,
                            onCheckedChange = { value ->
                                viewModel.updateCalculatorInput { it.copy(fatherAlive = value) }
                            },
                            modifier = Modifier.testTag("father_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الأم على قيد الحياة", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = input.motherAlive,
                            onCheckedChange = { value ->
                                viewModel.updateCalculatorInput { it.copy(motherAlive = value) }
                            },
                            modifier = Modifier.testTag("mother_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("عدد الإخوة والأخوات للمتوفى", style = MaterialTheme.typography.bodyMedium)
                            Text("يؤثر على انتقال الأم من الثلث للسدس فرضا", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (input.siblingsCount > 0) {
                                        viewModel.updateCalculatorInput { it.copy(siblingsCount = input.siblingsCount - 1) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "نقص")
                            }
                            Text("${input.siblingsCount}", style = MaterialTheme.typography.bodyLarge)
                            IconButton(
                                onClick = {
                                    viewModel.updateCalculatorInput { it.copy(siblingsCount = input.siblingsCount + 1) }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة")
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Children (الفروع والنسل الحقيقي)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الفروع (الأولاد المباشرون)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عدد الأبناء الذكور (الابن العاصب بالنفس)", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (input.sonsCount > 0) {
                                        viewModel.updateCalculatorInput { it.copy(sonsCount = input.sonsCount - 1) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "نقص")
                            }
                            Text("${input.sonsCount}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("sons_count_text"))
                            IconButton(
                                onClick = {
                                    viewModel.updateCalculatorInput { it.copy(sonsCount = input.sonsCount + 1) }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عدد البنات الإناث (صاحبات فرض أو عصبة بالغير)", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (input.daughtersCount > 0) {
                                        viewModel.updateCalculatorInput { it.copy(daughtersCount = input.daughtersCount - 1) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "نقص")
                            }
                            Text("${input.daughtersCount}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("daughters_count_text"))
                            IconButton(
                                onClick = {
                                    viewModel.updateCalculatorInput { it.copy(daughtersCount = input.daughtersCount + 1) }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة")
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Al-Tanzil Grandchildren (التنزيل الأحفاد)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الأحفاد المستحقين للتنزيل (المواد 169-172)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تطبيق التنزيل للأحفاد الأيتام", style = MaterialTheme.typography.bodyMedium)
                            Text("أولاد الميت في حياة جده/جدته", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Switch(
                            checked = input.hasTanzilGrandchildren,
                            onCheckedChange = { value ->
                                viewModel.updateCalculatorInput { it.copy(hasTanzilGrandchildren = value) }
                            },
                            modifier = Modifier.testTag("tanzil_switch")
                        )
                    }

                    if (input.hasTanzilGrandchildren) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جنس والدهم المتوفى في حياة والديه:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = input.tanzilParentIsSon,
                                onClick = {
                                    viewModel.updateCalculatorInput { it.copy(tanzilParentIsSon = true) }
                                }
                            )
                            Text("ابن ميت (يعصب أولاده)", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = !input.tanzilParentIsSon,
                                onClick = {
                                    viewModel.updateCalculatorInput { it.copy(tanzilParentIsSon = false) }
                                }
                            )
                            Text("بنت ميتة", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عدد أبناء الابن الأيتام (الذكور)", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (input.tanzilSonsCount > 0) {
                                            viewModel.updateCalculatorInput { it.copy(tanzilSonsCount = input.tanzilSonsCount - 1) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null)
                                }
                                Text("${input.tanzilSonsCount}", style = MaterialTheme.typography.bodyLarge)
                                IconButton(
                                    onClick = {
                                        viewModel.updateCalculatorInput { it.copy(tanzilSonsCount = input.tanzilSonsCount + 1) }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عدد بنات الابن الأيتام (الإناث)", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (input.tanzilDaughtersCount > 0) {
                                            viewModel.updateCalculatorInput { it.copy(tanzilDaughtersCount = input.tanzilDaughtersCount - 1) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null)
                                }
                                Text("${input.tanzilDaughtersCount}", style = MaterialTheme.typography.bodyLarge)
                                IconButton(
                                    onClick = {
                                        viewModel.updateCalculatorInput { it.copy(tanzilDaughtersCount = input.tanzilDaughtersCount + 1) }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Computed Output (النتائج)
        if (result != null && result.shareItems.isNotEmpty()) {
            item {
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جدول توزيع الفريضة الحسابية",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("أصل المسألة الأولي", style = MaterialTheme.typography.bodySmall)
                            Text("${result.originalBase}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("أصل المسألة النهائي", style = MaterialTheme.typography.bodySmall)
                            Text("${result.finalBase}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("العول / الرد", style = MaterialTheme.typography.bodySmall)
                            Text(result.isAulText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            items(result.shareItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.relativeCategory == "فرض") MaterialTheme.colorScheme.primary 
                                            else if (item.relativeCategory == "تنزيل") MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.secondary
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.relativeArabicName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", item.percentage)}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "طريقة الإرث: ${item.relativeCategory}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "حصته: " + if(item.relativeCategory == "تعصيب" || item.relativeCategory == "تنزيل" || item.totalShares == 0) item.finalFraction else "${item.shareShares}/${item.totalShares}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مسائل وخطوات الحل الفقهي والحسابي:", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        result.calculationSteps.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• $step",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("قم بتحديد فرد واحد على الأقل لتفعيل الحساب التلقائي المباشر.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. INTERACTIVE QUIZ TAB (اختبار تفاعلي)
// ==========================================

@Composable
fun QuizScreen(uiState: UiState, viewModel: MawarithViewModel) {
    val quiz = uiState.quizState
    val questions = QuizDatabase.questions

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "اختبار تفاعلي يحاكي البكالوريا الجزائرية",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "أسئلة ذكية مصاغة بعناية حول الميراث، الوقف، موانع الإرث والتنزيل لقياس استيعابك للمادة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!quiz.isQuizFinished) {
            val question = questions[quiz.currentQuestionIndex]

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "السؤال ${quiz.currentQuestionIndex + 1} من ${questions.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "النقاط: ${quiz.score}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (quiz.currentQuestionIndex + 1).toFloat() / questions.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(question.options.size) { index ->
                val optionText = question.options[index]
                val isSelected = quiz.selectedOptionIndex == index
                val isAnswered = quiz.isAnswered

                val containerColor = when {
                    isAnswered && index == question.correctIndex -> Color(0xFFD1FAE5) // Light Green
                    isAnswered && isSelected && index != question.correctIndex -> Color(0xFFFEE2E2) // Light Red
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    isAnswered && index == question.correctIndex -> Color(0xFF10B981)
                    isAnswered && isSelected && index != question.correctIndex -> Color(0xFFEF4444)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }

                val textColor = when {
                    isAnswered && index == question.correctIndex -> Color(0xFF065F46)
                    isAnswered && isSelected && index != question.correctIndex -> Color(0xFF991B1B)
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(!isAnswered) {
                            viewModel.selectQuizOption(index)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                    border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                        width = if (isSelected || (isAnswered && index == question.correctIndex)) 2.dp else 1.dp,
                        brush = Brush.linearGradient(listOf(borderColor, borderColor))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(index + 65).toChar()}. ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAnswered && index == question.correctIndex) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "صحيحة", tint = Color(0xFF10B981))
                        } else if (isAnswered && isSelected && index != question.correctIndex) {
                            Icon(Icons.Default.Cancel, contentDescription = "خاطئة", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.submitQuizAnswer() },
                        enabled = quiz.selectedOptionIndex != -1 && !quiz.isAnswered,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("submit_answer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("تأكيد الإجابة", fontWeight = FontWeight.Bold)
                    }

                    if (quiz.isAnswered) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { viewModel.nextQuizQuestion() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("السؤال التالي", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "تهانينا! أكملت الاختبار التفاعلي.",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لقد أجبت بشكل صحيح على ${quiz.score} من أصل ${questions.size} أسئلة.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val scorePercentage = (quiz.score.toFloat() / questions.size) * 100
                        val verdict = if (scorePercentage >= 50) "أداء ممتاز! أنت مستعب للبكالوريا." else "ننصحك بمراجعة ملخصات الدروس والعودة من جديد."
                        Text(
                            verdict,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.restartQuiz() },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("إعادة تشغيل الاختبار", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal dialog explaining the answer after verification
    if (quiz.showExplanationDialog) {
        val question = questions[quiz.currentQuestionIndex]
        Dialog(onDismissRequest = { viewModel.closeExplanation() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .animateContentSize()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التوضيح التفصيلي للبكالوريا",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.closeExplanation() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("تم، حسناً")
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SMART AI COMPANION (المساعد الذكي)
// ==========================================

@Composable
fun AiChatScreen(uiState: UiState, viewModel: MawarithViewModel) {
    var txtInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Scroll chat to bottom whenever a new message arrives
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "المساعد الذكي في علم الميراث",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "اسأل عن الوقف، مسائل الحجب، أو اطلب حل مسألة مخصصة وسيقوم الذكاء الاصطناعي بشرحها تفصيلياً.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Warning if Gemini Key is absent
        if (!uiState.isApiKeyPresent) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مفتاح Gemini API غير مكوَّن في لوحة أسرار AI Studio. تم تمكين المساعد المحلي الذكي بدلاً منه تلقائياً.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Suggestions tags
        val suggestions = listOf("اشرح الكلالة بالتفصيل", "قواعد تنزيل الأحفاد", "مسألة الغراوين للأم", "الفرق بين الوقف والميراث")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable {
                            viewModel.sendChatMessage(tag)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Chat conversation bubble list
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.chatMessages) { message ->
                val isUser = message.sender == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    val bubbleBgColor = if (isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val bubbleTextColor = if (isUser) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = bubbleBgColor),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .shadow(1.dp, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 18.sp,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = bubbleTextColor
                            )
                        }
                    }
                }
            }

            if (uiState.aiLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "يتأمل الذكاء الاصطناعي في الفريضة...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.aiError != null) {
                item {
                    Text(
                        text = uiState.aiError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Send message interactive row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = txtInput,
                onValueChange = { txtInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                placeholder = { Text("اطرح مسألتك أو سؤالك الشرعي هنا...", fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (txtInput.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(txtInput)
                            txtInput = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            IconButton(
                onClick = {
                    if (txtInput.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(txtInput)
                        txtInput = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("send_chat_button"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
