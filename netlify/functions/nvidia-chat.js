const NVIDIA_KEY = "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";
const NVIDIA_URL = "https://integrate.api.nvidia.com/v1/chat/completions";

exports.handler = async (event) => {
  // Only allow POST
  if (event.httpMethod !== "POST") {
    return {
      statusCode: 405,
      body: JSON.stringify({ error: "Method Not Allowed" }),
    };
  }

  try {
    const body = JSON.parse(event.body);
    const userMessage = body.message;

    if (!userMessage) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "Missing message field" }),
      };
    }

    const SITE_CONTEXT = `אתה עוזר מידע לאתר "Galil 360" - רובע עסקי חדש לעסקים והייטק בנהריה. הנה המידע הרלוונטי מהאתר:

    Galil 360 - Health & Technology District, נהריה.
    הרובע העסקי החדש של הצפון. פיתוח בעיצומו, מגרשים לשיווק ויזמות במיקום האסטרטגי ביותר בגליל.

    נהריה כבסיס כלכלי איתן: העיר נהריה עוברת מהפכה כלכלית ואורבנית והופכת לבירת ההייטק והרפואה של הצפון. העיר מהווה בסיס כלכלי יציב וצומח, עם השקעות עתק בתשתיות, חינוך ופיתוח סביבתי.

    GALIL 360 מסמל את המעבר האסטרטגי של העיר ממודל תעשייתי מסורתי למרכז חדשנות עולמי מבוסס ידע. שיתוף הפעולה הייחודי בין הרשות המקומית, המרכז הרפואי לגליל והאקדמיה יוצר אקו-סיסטם מושלם לצמיחה עסקית.

    יתרונות הפארק:
    1. נגישות תחבורתית - מיקום אידיאלי בסמוך לתחנת הרכבת, כביש 4, וכביש 6 העתידי.
    2. חיבור למרכז רפואי - קרבה וסינרגיה חסרת תקדים עם המרכז הרפואי לגליל, אקו-סיסטם לחברות MedTech ו-BioTech.
    3. תמריצים ממשלתיים - הפארק ממוקם באזור פיתוח א', מענקי השקעה מהמדינה, הטבות מס משמעותיות, ותמריצים להעסקת עובדים.

    סטטוס: פיתוח בעיצומו. התוכניות מאושרות והקרקעות מוכנות לשיווק מיידי.

    תפיסת עיר 5 דקות: מרחב מעורב שימושים המשלב תעסוקה, מסחר, פנאי ושירותים קהילתיים במרחק הליכה קצר.

    איכות חיים: נהריה מציעה עיר חוף תוססת, קהילה איכותית וחיי פנאי עשירים - איזון אמיתי בין קריירה לחיים האישיים.

    קהלי יעד: יזמים וקבלנים, חברות טכנולוגיה ורפואה, משקיעים מוסדיים ופרטיים.

    מנהלת היישום כוללת: ראש העיר נהריה (יו"ר מנהלת הפארק), מנהל המרכז הרפואי (שותף אסטרטגי), מנכ"ל החברה הכלכלית (ניהול ופיתוח), מנהלת הפארקים (ליווי יזמים ופניות).

    חברי מנהלת: דימיטרי אפשטיין (ראש מנהלת, סגן ראש העיר), עו"ד דוד אלחייאני (מנכ"ל עיריית נהריה), טל חמי (מהנדס העיר), מיכה זנו (גזבר העירייה), מר רונן גרשטיין, מר עוז כץ, גב' נדיה פאעור (צוות אסטרטגי).

    ליצירת קשר וקביעת פגישה: benabuhila@gmail.com

    תענה בעברית, בקצרה ולעניין, עד 4-5 משפטים. היה ידידותי ומקצועי.`;

    const response = await fetch(NVIDIA_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${NVIDIA_KEY}`,
      },
      body: JSON.stringify({
        model: "meta/llama-3.3-70b-instruct",
        messages: [
          { role: "system", content: SITE_CONTEXT },
          { role: "user", content: userMessage },
        ],
        temperature: 0.7,
        max_tokens: 500,
        top_p: 1,
      }),
    });

    if (!response.ok) {
      const errText = await response.text();
      console.error("NVIDIA API error:", response.status, errText);
      return {
        statusCode: 502,
        body: JSON.stringify({ error: "שגיאה בחיבור ל-NVIDIA API" }),
      };
    }

    const data = await response.json();
    const reply =
      data.choices?.[0]?.message?.content ||
      "לא הצלחתי להפיק תשובה. אנא נסה שוב.";

    return {
      statusCode: 200,
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ reply }),
    };
  } catch (error) {
    console.error("Function error:", error);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: "שגיאה פנימית בשרת" }),
    };
  }
};