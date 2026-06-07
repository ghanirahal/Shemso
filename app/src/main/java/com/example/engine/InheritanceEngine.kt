package com.example.engine

import java.util.Locale

data class InheritanceInput(
    val hasHusband: Boolean = false,
    val hasWife: Boolean = false,
    val wivesCount: Int = 1,
    val sonsCount: Int = 0,
    val daughtersCount: Int = 0,
    val fatherAlive: Boolean = false,
    val motherAlive: Boolean = false,
    val siblingsCount: Int = 0,
    
    // Al-Tanzil (الوصية الواجبة) details for grandchildren through deceased children:
    val hasTanzilGrandchildren: Boolean = false,
    val tanzilParentIsSon: Boolean = true, // true if deceased parent was a son
    val tanzilSonsCount: Int = 0, // count of grandson(s)
    val tanzilDaughtersCount: Int = 0 // count of granddaughter(s)
)

data class ShareItem(
    val relativeArabicName: String,
    val relativeCategory: String, // "فرض" or "تعصيب" or "تنزيل"
    val baseFraction: String, // e.g. "1/8", "1/6", "الباقي تعصيباً"
    val finalFraction: String, // simplified if possible, e.g. "3/24"
    val shareShares: Int, // allocated shares
    val totalShares: Int, // total base
    val percentage: Double,
    val explanation: String
)

data class InheritanceResult(
    val shareItems: List<ShareItem>,
    val originalBase: Int,
    val finalBase: Int,
    val isAulText: String, // "عول" or "لا يوجد عول" or "رد"
    val isTanzilApplied: Boolean,
    val tanzilShareFraction: String,
    val calculationSteps: List<String>
)

object InheritanceEngine {

    // Help find the greatest common divisor for simplifying fractions
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    private fun getRelativeGCD(denominators: List<Int>): Int {
        if (denominators.isEmpty()) return 1
        var result = denominators[0]
        for (i in 1 until denominators.size) {
            val h = gcd(result, denominators[i])
            result = (result * denominators[i]) / h
        }
        return result
    }

    // Standard engine which takes inputs and calculates shares out of a base
    fun calculate(input: InheritanceInput): InheritanceResult {
        val steps = mutableListOf<String>()
        steps.add("بدء عملية فحص الورثة وتحديد نسبهم الشرعية.")

        // 1. If Al-Tanzil applies, retrieve the deceased parent's hypothetical share
        var tanzilApplied = false
        var tanzilFractionText = "0"
        var tanzilShareOfEstate = 0.0 // from 0.0 to 1.0 (limit to 1/3)
        var tanzilSons = input.tanzilSonsCount
        var tanzilDaths = input.tanzilDaughtersCount

        if (input.hasTanzilGrandchildren && (tanzilSons > 0 || tanzilDaths > 0)) {
            steps.add("تطبيق نظام التنزيل (المواد 169-172 من قانون الأسرة الجزائري):")
            steps.add("خطوة 1: تقييم حصة والد/والدة الأحفاد المتوفى افتراضياً كأنه حي وقت الوفاة.")

            // Configure hypothetical scenario input: Deceased parent is alive
            val hypoInput = InheritanceInput(
                hasHusband = input.hasHusband,
                hasWife = input.hasWife,
                wivesCount = input.wivesCount,
                fatherAlive = input.fatherAlive,
                motherAlive = input.motherAlive,
                siblingsCount = input.siblingsCount,
                sonsCount = input.sonsCount + (if (input.tanzilParentIsSon) 1 else 0),
                daughtersCount = input.daughtersCount + (if (!input.tanzilParentIsSon) 1 else 0),
                hasTanzilGrandchildren = false // no nested tanzil
            )

            val hypoResult = calculateSimple(hypoInput)
            val parentName = if (input.tanzilParentIsSon) "الابن المتوفى (افتراضاً)" else "البنت المتوفاة (افتراضاً)"
            val parentShareItem = hypoResult.shareItems.find { it.relativeArabicName.startsWith(parentName) }
            
            if (parentShareItem != null) {
                val rawParentShare = parentShareItem.percentage / 100.0
                steps.add("نصيب أصل الأحفاد الافتراضي في التركة هو: ${parentShareItem.baseFraction} (ما يعادل ${String.format(Locale.US, "%.1f", parentShareItem.percentage)}%).")
                
                // Cap to 1/3
                if (rawParentShare >= 1.0 / 3.0) {
                    tanzilShareOfEstate = 1.0 / 3.0
                    tanzilFractionText = "1/3 (الحد الأقصى للوصية الواجبة)"
                    steps.add("النتيجة تجاوزت الثلث، ولذلك تُرد الوصية الواجبة (التنزيل) إلى سقف الثلث القانوني (1/3).")
                } else {
                    tanzilShareOfEstate = rawParentShare
                    tanzilFractionText = parentShareItem.baseFraction
                    steps.add("النتيجة لم تتجاوز الثلث، لذلك تستحق بنت/أولاد الابن حصة معادلها بالكامل: $tanzilFractionText.")
                }
                tanzilApplied = true
            } else {
                steps.add("فشل تحديد حصة الأصل الافتراضي، تم إلغاء تنشيط التنزيل.")
            }
        }

        // Base calculation on remaining estate
        val multiplier = if (tanzilApplied) (1.0 - tanzilShareOfEstate) else 1.0
        val baseResult = calculateSimple(input)

        val finalShareItems = mutableListOf<ShareItem>()
        steps.add("خطوة 2: توزيع الحصة الحقيقية المتبقية (${String.format(Locale.US, "%.1f", multiplier * 100)}%) على الورثة الشرعيين.")

        if (tanzilApplied) {
            // Add Tanzil heirs first
            val tanzilTotalPercent = tanzilShareOfEstate * 100.0
            val tanzilExplain = "تنزيل منزلة أصلهم المتوفى مع سقف الثلث."
            
            // Grandchildren divide using للذكر مثل حظ الأنثيين
            val parts = tanzilSons * 2 + tanzilDaths
            if (parts > 0) {
                if (tanzilSons > 0) {
                    val sPercent = (tanzilTotalPercent * 2.0 / parts) / tanzilSons
                    val rawSFraction = "ابن ابن متوفى"
                    finalShareItems.add(
                        ShareItem(
                            relativeArabicName = "أولاد الابن المتوفى (الذكور وعددهم $tanzilSons)",
                            relativeCategory = "تنزيل",
                            baseFraction = "تنزيل ($tanzilFractionText)",
                            finalFraction = "${String.format(Locale.US, "%.2f", sPercent * tanzilSons)}%",
                            shareShares = tanzilSons * 2,
                            totalShares = parts,
                            percentage = sPercent * tanzilSons,
                            explanation = "يقسم سهم التنزيل للذكر مثل حظ الأنثيين بالتساوي بين $tanzilSons من الأبناء."
                        )
                    )
                }
                if (tanzilDaths > 0) {
                    val dPercent = (tanzilTotalPercent / parts) / tanzilDaths
                    finalShareItems.add(
                        ShareItem(
                            relativeArabicName = "أولاد الابن المتوفى (الإناث وعددهن $tanzilDaths)",
                            relativeCategory = "تنزيل",
                            baseFraction = "تنزيل ($tanzilFractionText)",
                            finalFraction = "${String.format(Locale.US, "%.2f", dPercent * tanzilDaths)}%",
                            shareShares = tanzilDaths,
                            totalShares = parts,
                            percentage = dPercent * tanzilDaths,
                            explanation = "يقسم سهم التنزيل للذكر مثل حظ الأنثيين بالتساوي بين $tanzilDaths من البنات."
                        )
                    )
                }
            }
        }

        // Add real heirs adjusted by multiplier
        for (item in baseResult.shareItems) {
            val adjustedPercent = item.percentage * multiplier
            val adjFraction = if (tanzilApplied) {
                "${String.format(Locale.US, "%.1f", adjustedPercent)}%"
            } else {
                item.finalFraction
            }
            
            finalShareItems.add(
                ShareItem(
                    relativeArabicName = item.relativeArabicName,
                    relativeCategory = item.relativeCategory,
                    baseFraction = item.baseFraction,
                    finalFraction = adjFraction,
                    shareShares = item.shareShares,
                    totalShares = item.totalShares,
                    percentage = adjustedPercent,
                    explanation = item.explanation + (if (tanzilApplied) " (معدل بعد اقتطاع التنزيل)" else "")
                )
            )
        }

        steps.addAll(baseResult.calculationSteps)

        return InheritanceResult(
            shareItems = finalShareItems,
            originalBase = baseResult.originalBase,
            finalBase = baseResult.finalBase,
            isAulText = baseResult.isAulText,
            isTanzilApplied = tanzilApplied,
            tanzilShareFraction = tanzilFractionText,
            calculationSteps = steps
        )
    }

    // Help compute clean shares without Tanzil (base method)
    private fun calculateSimple(input: InheritanceInput): InheritanceResult {
        val steps = mutableListOf<String>()
        val hasChildren = input.sonsCount > 0 || input.daughtersCount > 0
        val isMaleBranchPresent = input.sonsCount > 0
        val isFemaleBranchOnly = input.daughtersCount > 0 && input.sonsCount == 0

        // Determine Fards
        val fards = mutableMapOf<String, FractionalShare>()

        // 1. Spouses
        if (input.hasHusband) {
            if (hasChildren) {
                fards["الزوج"] = FractionalShare(1, 4, "مفروض الربع لوجود فرع وارث له")
            } else {
                fards["الزوج"] = FractionalShare(1, 2, "مفروض النصف لعدم وجود فرع وارث له")
            }
        } else if (input.hasWife) {
            val d = if (hasChildren) 8 else 4
            val label = if (hasChildren) "مفروض الثمن لوجود فرع وارث" else "مفروض الربع لعدم وجود فرع وارث"
            val suffix = if (input.wivesCount > 1) " (يوزع بالتساوي بين الزوجات والعدد ${input.wivesCount})" else ""
            fards["الزوجة/الزوجات (العدد ${input.wivesCount})"] = FractionalShare(1, d, label + suffix)
        }

        // 2. Mother
        if (input.motherAlive) {
            if (hasChildren || input.siblingsCount >= 2) {
                fards["الأم"] = FractionalShare(1, 6, "مفروض السدس لوجود فرع وارث أو عدد من الإخوة")
            } else if (input.fatherAlive && (input.hasHusband || input.hasWife) && !hasChildren) {
                // Gharra'ayn (1/3 of remainder)
                fards["الأم"] = FractionalShare(1, 4, "ثلث الباقي (مسألة غراوين) لوجود زوج وأب")
            } else {
                fards["الأم"] = FractionalShare(1, 3, "مفروض الثلث لعدم وجود فرع وارث ولا عدد من الإخوة")
            }
        }

        // 3. Father Fard check
        if (input.fatherAlive) {
            if (isMaleBranchPresent) {
                fards["الأب"] = FractionalShare(1, 6, "مفروض السدس لوجود فرع وارث مذكر (الابن)")
            } else if (isFemaleBranchOnly) {
                fards["الأب"] = FractionalShare(1, 6, "مفروض السدس فرضا زائد الباقي تعصيباً")
            }
        }

        // 4. Daughters Fard check (only if no sons)
        if (isFemaleBranchOnly) {
            if (input.daughtersCount == 1) {
                fards["البنت الوحيدة"] = FractionalShare(1, 2, "مفروض النصف لأنها واحدة غير معصبة")
            } else if (input.daughtersCount >= 2) {
                fards["البنات (العدد ${input.daughtersCount})"] = FractionalShare(2, 3, "مفروض الثلثين للتعدد وعدم وجود معصب")
            }
        }

        // Compute Base of the estate
        val denominators = fards.values.map { it.denom }
        val originalBase = getRelativeGCD(denominators)
        
        // Distribute initial shares out of base
        val initialShares = mutableMapOf<String, Int>()
        var sumFards = 0
        for ((name, f) in fards) {
            val allocated = (originalBase / f.denom) * f.num
            initialShares[name] = allocated
            sumFards += allocated
        }

        steps.add("أكسار الفروض الموزعة: " + fards.map { "${it.key}: ${it.value.num}/${it.value.denom}" }.joinToString(" | "))
        steps.add("أصل المسألة الأولي المستخرج: $originalBase.")

        var finalBase = originalBase
        var isAulText = "لا يوجد عول"

        if (sumFards > originalBase) {
            // Aul occurs! The base expands to sum of shares
            finalBase = sumFards
            isAulText = "عول"
            steps.add("ازدحام الفروض: مجموع سهام ذوي الفروض ($sumFards) تجاوز أصل المسألة الإجمالي وهو ($originalBase).")
            steps.add("تحقق العول: اتساع الفريضة لتعول من $originalBase إلى $sumFards، مما يخفض نسب الجميع بالتساوي لدفع الضرر.")
        }

        val shareItems = mutableListOf<ShareItem>()

        // Put initial fard shares into return list
        for ((name, s) in initialShares) {
            val f = fards[name]!!
            val pct = (s.toDouble() / finalBase.toDouble()) * 100.0
            val normGcd = gcd(s, finalBase)
            val simplified = "${s / normGcd}/${finalBase / normGcd}"
            shareItems.add(
                ShareItem(
                    relativeArabicName = name,
                    relativeCategory = "فرض",
                    baseFraction = "${f.num}/${f.denom}",
                    finalFraction = simplified,
                    shareShares = s,
                    totalShares = finalBase,
                    percentage = pct,
                    explanation = f.explanation
                )
            )
        }

        // Ta'asib distribution
        val leftoverShares = finalBase - sumFards
        if (leftoverShares > 0) {
            steps.add("تبقى عصبة مالية متبقية مقدارها $leftoverShares من أصل $finalBase سهام.")
            
            // Check Ta'asib heirs
            val hasSons = input.sonsCount > 0
            val hasDaughters = input.daughtersCount > 0
            
            if (hasSons) {
                // Sons & Daughters inherit by Ta'asib (للذكر مثل حظ الأنثيين)
                val totalPartsCount = input.sonsCount * 2 + input.daughtersCount
                val sonPartShares = (leftoverShares.toDouble() / totalPartsCount.toDouble()) * 2.0
                val dathPartShares = leftoverShares.toDouble() / totalPartsCount.toDouble()

                steps.add("توزيع الباقي تعصيباً على الأبناء والمنزلين (للذكر مثل حظ الأنثيين):")
                if (input.sonsCount > 0) {
                    val actualSharesValue = (leftoverShares.toDouble() * (input.sonsCount * 2)) / totalPartsCount
                    val pct = (actualSharesValue / finalBase) * 100.0
                    shareItems.add(
                        ShareItem(
                            relativeArabicName = "الأبناء الذكور (العدد ${input.sonsCount})",
                            relativeCategory = "تعصيب",
                            baseFraction = "الباقي تعصيباً",
                            finalFraction = "${String.format(Locale.US, "%.2f", pct)}%",
                            shareShares = sRound(actualSharesValue),
                            totalShares = finalBase,
                            percentage = pct,
                            explanation = "يرثون الباقي بالتعصيب مناصفة بالذكورة مع البنات."
                        )
                    )
                    steps.add("- للأبناء الذكور حصة متبقية تعصيباً تعادل ${String.format(Locale.US, "%.1f", pct)}%.")
                }
                if (input.daughtersCount > 0) {
                    val actualSharesValue = (leftoverShares.toDouble() * input.daughtersCount) / totalPartsCount
                    val pct = (actualSharesValue / finalBase) * 100.0
                    shareItems.add(
                        ShareItem(
                            relativeArabicName = "البنات (العدد ${input.daughtersCount})",
                            relativeCategory = "تعصيب",
                            baseFraction = "عصبة بالغير",
                            finalFraction = "${String.format(Locale.US, "%.2f", pct)}%",
                            shareShares = sRound(actualSharesValue),
                            totalShares = finalBase,
                            percentage = pct,
                            explanation = "يرثون الباقي بالتعصيب بالغير مع وجود شقيق ذكر."
                        )
                    )
                    steps.add("- للبنات حصة متبقية عصبة بالغير تعادل ${String.format(Locale.US, "%.1f", pct)}%.")
                }
            } else if (input.fatherAlive) {
                // Father inherits remaining by Ta'asib
                val pct = (leftoverShares.toDouble() / finalBase.toDouble()) * 100.0
                steps.add("يرث الأب الفضلة المتبقية ($leftoverShares سهام) بالتعصيب لعدم وجود فرع وارث مذكر.")
                
                // If father already exists in fards (because of daughters), we combine or keep it separate
                val existingFather = shareItems.find { it.relativeArabicName == "الأب" }
                if (existingFather != null) {
                    // Update
                    val index = shareItems.indexOf(existingFather)
                    val newShares = existingFather.shareShares + leftoverShares
                    val newPct = (newShares.toDouble() / finalBase) * 100.0
                    val normGcd = gcd(newShares, finalBase)
                    shareItems[index] = ShareItem(
                        relativeArabicName = "الأب",
                        relativeCategory = "فرض + تعصيب",
                        baseFraction = "Suds + Taasib",
                        finalFraction = "${newShares / normGcd}/${finalBase / normGcd}",
                        shareShares = newShares,
                        totalShares = finalBase,
                        percentage = newPct,
                        explanation = "أخذ السدس فرضاً لوجود الفرع الوارث المؤنث، وحاز الباقي بالتعصيب."
                    )
                } else {
                    val normGcd = gcd(leftoverShares, finalBase)
                    shareItems.add(
                        ShareItem(
                            relativeArabicName = "الأب",
                            relativeCategory = "تعصيب",
                            baseFraction = "الباقي تعصيباً",
                            finalFraction = "${leftoverShares / normGcd}/${finalBase / normGcd}",
                            shareShares = leftoverShares,
                            totalShares = finalBase,
                            percentage = pct,
                            explanation = "الوالد يستحق الباقي تعصيباً لعدم وجود فرع وارث مذكر."
                        )
                    )
                }
            } else {
                // Radd applies (رد) or other relatives (not detailed, we assume Radd back to standard if no other heirs)
                isAulText = "رد"
                steps.add("تطبيق فريضة الرد: وجود فضلة مالية مع عدم وجود عاصب، فترد الأسهم على ذوي الفروض ما عدا الزوجين.")
                
                // Redistribute excluding husband/wife
                val totalRaddShares = shareItems.filter { it.relativeArabicName != "الزوج" && !it.relativeArabicName.startsWith("الزوجة") }
                    .sumOf { it.shareShares }
                
                if (totalRaddShares > 0) {
                    val hasSpouse = input.hasHusband || input.hasWife
                    val spouseItem = shareItems.find { it.relativeArabicName == "الزوج" || it.relativeArabicName.startsWith("الزوجة") }
                    val spouseShares = spouseItem?.shareShares ?: 0
                    
                    // Ratio reallocation
                    val baseFactor = if (hasSpouse) (finalBase - spouseShares) else finalBase
                    val multiplier = if (hasSpouse) (baseFactor.toDouble() / totalRaddShares) else 1.0
                    
                    for (i in shareItems.indices) {
                        val item = shareItems[i]
                        if (item.relativeArabicName != "الزوج" && !item.relativeArabicName.startsWith("الزوجة")) {
                            val newSharesSum = item.shareShares * multiplier
                            val adjustedPct = (newSharesSum / finalBase) * 100.0
                            shareItems[i] = item.copy(
                                percentage = adjustedPct,
                                finalFraction = "${String.format(Locale.US, "%.1f", adjustedPct)}%",
                                explanation = item.explanation + " (مع الرد)"
                            )
                        }
                    }
                }
            }
        }

        return InheritanceResult(
            shareItems = shareItems,
            originalBase = originalBase,
            finalBase = finalBase,
            isAulText = isAulText,
            isTanzilApplied = false,
            tanzilShareFraction = "0",
            calculationSteps = steps
        )
    }

    private fun sRound(v: Double): Int {
        return Math.round(v).toInt()
    }
}

data class FractionalShare(
    val num: Int,
    val denom: Int,
    val explanation: String
)
