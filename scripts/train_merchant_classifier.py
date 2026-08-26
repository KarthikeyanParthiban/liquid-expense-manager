#!/usr/bin/env python3
"""
On-Device Merchant Classifier Training Script
Trains a Log-Linear Softmax Model with character n-grams and word tokens
for classifying Indian merchant names and transaction descriptions into 13 expense categories.
Exports model weights directly to JSON for ultra-fast on-device native Kotlin inference.
"""

import json
import os
import re
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import FeatureUnion

CATEGORIES = [
    "FOOD",
    "GROCERIES",
    "SHOPPING",
    "TRANSPORT",
    "BILLS_UTILITIES",
    "ENTERTAINMENT",
    "HEALTHCARE",
    "INVESTMENT",
    "SALARY_INCOME",
    "TRANSFERS",
    "FEES_CHARGES",
    "EDUCATION",
    "PERSONAL",
    "OTHERS"
]

# Training samples covering diverse Indian merchants, local shops, VPAs, and unstructured text
DATASET = [
    # FOOD
    ("Sharma Sweets House", "FOOD"),
    ("Chai Point Express", "FOOD"),
    ("Amma Canteen", "FOOD"),
    ("Bawarchi Biryani Point", "FOOD"),
    ("Madras Cafe & Tiffin", "FOOD"),
    ("Gupta Dosa Corner", "FOOD"),
    ("Hot & Spicy Fast Food", "FOOD"),
    ("Kathi Roll Junction", "FOOD"),
    ("Gokul Chaat Bhandar", "FOOD"),
    ("Haldiram Sweets & Snacks", "FOOD"),
    ("Saravana Bhavan Hotel", "FOOD"),
    ("Paradise Food Court", "FOOD"),
    ("Annapurna Mess & Meals", "FOOD"),
    ("Sri Krishna Sweets", "FOOD"),
    ("Kwality Ice Cream Parlour", "FOOD"),
    ("Fresh Juice Center", "FOOD"),
    ("Bakingo Cake Bakery", "FOOD"),
    ("Theobroma Patisserie", "FOOD"),
    ("Burger House & Shakes", "FOOD"),
    ("Tandoor Hut Restro", "FOOD"),
    ("Bhojanalaya & Thali", "FOOD"),
    ("Punjab Dhaba Highway", "FOOD"),
    ("Coffee Day Lounge", "FOOD"),
    ("Star Cafe Bistro", "FOOD"),
    ("Shawarma King Corner", "FOOD"),
    ("Momo Nation Cafe", "FOOD"),
    ("Pizza Bite Corner", "FOOD"),
    ("Waffle World Treats", "FOOD"),
    ("Cookie Dough Bakery", "FOOD"),
    ("Kebab Gali Resto", "FOOD"),
    ("South Indian Tiffin Center", "FOOD"),
    ("Royal Kitchen Restaurant", "FOOD"),
    ("Bikaner Sweets Corner", "FOOD"),
    ("Rasoi Ghar Dining", "FOOD"),
    ("Chai Nashta Point", "FOOD"),
    ("Sweet Bengal Mithai", "FOOD"),
    ("chaipoint@icici", "FOOD"),
    ("swiggy_instafood@hdfcbank", "FOOD"),
    ("zomato_pay@paytm", "FOOD"),
    ("mcdonalds@axisbank", "FOOD"),
    ("kfc_foods@sbi", "FOOD"),

    # GROCERIES
    ("Gupta Kirana Store", "GROCERIES"),
    ("Balaji Provisions & Gen Store", "GROCERIES"),
    ("Patanjali Chikitsalaya Store", "GROCERIES"),
    ("Fresh Vegetable & Fruit Mart", "GROCERIES"),
    ("Daily Needs Supermarket", "GROCERIES"),
    ("Kisan Organic Greens", "GROCERIES"),
    ("Super Bazaar Grocery", "GROCERIES"),
    ("Nandini Milk Dairy Booth", "GROCERIES"),
    ("Heritage Milk & Curd Parlour", "GROCERIES"),
    ("Sri Lakshmi Rice Mill", "GROCERIES"),
    ("Fish Meat & Chicken Center", "GROCERIES"),
    ("Royal Dry Fruits & Spices", "GROCERIES"),
    ("Apna Supermarket Wholesale", "GROCERIES"),
    ("Blinkit Groceries Express", "GROCERIES"),
    ("Zepto 10 Min Grocery", "GROCERIES"),
    ("Instamart Fresh Daily", "GROCERIES"),
    ("BB Daily Fresh Milk", "GROCERIES"),
    ("Ratnadeep Supermarket", "GROCERIES"),
    ("More Retail Supermarket", "GROCERIES"),
    ("Spencers Daily Groceries", "GROCERIES"),
    ("Nature Basket Gourmet", "GROCERIES"),
    ("FreshToHome Fish Chicken", "GROCERIES"),
    ("Licious Fresh Cuts", "GROCERIES"),
    ("Vegetable Mandi Vendor", "GROCERIES"),
    ("Grain & Flour Merchant", "GROCERIES"),
    ("Atta Dal Oil Kirana", "GROCERIES"),
    ("Modern Bazaar Grocery", "GROCERIES"),
    ("Mother Dairy Milk Booth", "GROCERIES"),
    ("Amul Icecream & Dairy", "GROCERIES"),
    ("Aavin Milk Depot", "GROCERIES"),

    # SHOPPING
    ("Shree Balaji Electronics & Mobiles", "SHOPPING"),
    ("Fashion Hub Mens Wear", "SHOPPING"),
    ("Trends Footwear & Bags", "SHOPPING"),
    ("Mahalaxmi Jewellers & Gold", "SHOPPING"),
    ("Kalyan Jewellers Showroom", "SHOPPING"),
    ("Tanishq Jewellery Boutique", "SHOPPING"),
    ("Sri Saree Mandir Textiles", "SHOPPING"),
    ("Mobile Care & Accessories", "SHOPPING"),
    ("National Book Depot & Stationers", "SHOPPING"),
    ("Titan Eye Plus Opticals", "SHOPPING"),
    ("Lenskart Eyewear Frame", "SHOPPING"),
    ("Hardware & Sanitary Fittings", "SHOPPING"),
    ("Royal Furniture & Mattress", "SHOPPING"),
    ("Crossword Books & Gifts", "SHOPPING"),
    ("Decathlon Sports Gear", "SHOPPING"),
    ("Nike Store Shoes", "SHOPPING"),
    ("Adidas Originals Showroom", "SHOPPING"),
    ("Puma Factory Outlet", "SHOPPING"),
    ("Zara Clothing Apparel", "SHOPPING"),
    ("H&M Fashion Lifestyle", "SHOPPING"),
    ("FabIndia Ethnic Wear", "SHOPPING"),
    ("Manyavar Mens Ethnic", "SHOPPING"),
    ("Croma Electronics Retail", "SHOPPING"),
    ("Reliance Digital Gadgets", "SHOPPING"),
    ("Vijay Sales Appliance", "SHOPPING"),
    ("Poorvika Mobiles Store", "SHOPPING"),
    ("Sangeetha Mobiles", "SHOPPING"),
    ("Hamleys Toy Store", "SHOPPING"),
    ("FirstCry Baby Products", "SHOPPING"),
    ("Nykaa Beauty Cosmetics", "SHOPPING"),
    ("Purplle Makeup Essentials", "SHOPPING"),
    ("Shoppers Stop Departmental", "SHOPPING"),
    ("Lifestyle Stores Fashion", "SHOPPING"),
    ("Westside Trent Retail", "SHOPPING"),
    ("Pantaloons Fashion Store", "SHOPPING"),
    ("Zudio Budget Wear", "SHOPPING"),

    # TRANSPORT
    ("Ramesh Cabs Travels", "TRANSPORT"),
    ("Fast Track City Taxi", "TRANSPORT"),
    ("Indian Oil Petrol Bunk", "TRANSPORT"),
    ("HP Petrol Pump Fuel", "TRANSPORT"),
    ("Bharat Petroleum BPCL Station", "TRANSPORT"),
    ("Shell Fuel Station", "TRANSPORT"),
    ("Toll Plaza NHAI Fastag", "TRANSPORT"),
    ("IHMCL Fastag Auto Toll", "TRANSPORT"),
    ("Metro Rail Transit Smartcard", "TRANSPORT"),
    ("Auto Driver Ride UPI", "TRANSPORT"),
    ("Bus Depot Ticket Booking", "TRANSPORT"),
    ("Parking Lot Valet Fee", "TRANSPORT"),
    ("Ather EV Charging Station", "TRANSPORT"),
    ("Tata Power EV Charge", "TRANSPORT"),
    ("Zoomcar Vehicle Rental", "TRANSPORT"),
    ("Revv Self Drive Cars", "TRANSPORT"),
    ("Royal Enfield Service Center", "TRANSPORT"),
    ("Maruti Suzuki Car Service", "TRANSPORT"),
    ("Hero MotoCorp Workshop", "TRANSPORT"),
    ("MRF Tyres & Wheel Alignment", "TRANSPORT"),
    ("Speedway Petrol Station", "TRANSPORT"),
    ("Ola Cabs Auto Ride", "TRANSPORT"),
    ("Uber Trip Transit", "TRANSPORT"),
    ("Rapido Bike Taxi", "TRANSPORT"),
    ("Namma Yatri Auto Ride", "TRANSPORT"),
    ("BluSmart Electric Cab", "TRANSPORT"),

    # BILLS_UTILITIES
    ("Electricity Board Bescom", "BILLS_UTILITIES"),
    ("TANGEDCO Power Bill", "BILLS_UTILITIES"),
    ("MSEDCL Electricity Payment", "BILLS_UTILITIES"),
    ("Indane Gas Cylinder Booking", "BILLS_UTILITIES"),
    ("Bharat Gas Refill Agency", "BILLS_UTILITIES"),
    ("HP Gas LPG Agency", "BILLS_UTILITIES"),
    ("Municipal Water Supply Board", "BILLS_UTILITIES"),
    ("Airtel Fiber Broadband Internet", "BILLS_UTILITIES"),
    ("Jio Fiber Broadband Bill", "BILLS_UTILITIES"),
    ("ACT Fibernet Monthly Bill", "BILLS_UTILITIES"),
    ("Tata Play DTH Recharge", "BILLS_UTILITIES"),
    ("Airtel Digital TV Recharge", "BILLS_UTILITIES"),
    ("Sun Direct DTH Payment", "BILLS_UTILITIES"),
    ("Airtel Postpaid Mobile Bill", "BILLS_UTILITIES"),
    ("Jio Postpaid Mobile Bill", "BILLS_UTILITIES"),
    ("Vodafone Idea Vi Postpaid", "BILLS_UTILITIES"),
    ("Apartment Maintenance Fee", "BILLS_UTILITIES"),
    ("Property Tax Municipal Corp", "BILLS_UTILITIES"),
    ("Piped Natural Gas IGL Bill", "BILLS_UTILITIES"),
    ("MGL Piped Gas Payment", "BILLS_UTILITIES"),
    ("Adani Electricity Mumbai", "BILLS_UTILITIES"),
    ("Torrent Power Electricity", "BILLS_UTILITIES"),
    ("Hathway Cable & Internet", "BILLS_UTILITIES"),

    # ENTERTAINMENT
    ("PVR Cinemas Movie Ticket", "ENTERTAINMENT"),
    ("Inox Leisure Cinema", "ENTERTAINMENT"),
    ("Cinepolis Multiplex Screens", "ENTERTAINMENT"),
    ("Miraj Cinemas Movie", "ENTERTAINMENT"),
    ("Fun World Amusement Park", "ENTERTAINMENT"),
    ("Wonderla Holidays Theme Park", "ENTERTAINMENT"),
    ("Game Zone Arcade Gaming", "ENTERTAINMENT"),
    ("Timezone Bowling Alley", "ENTERTAINMENT"),
    ("Smaaash Sports Gaming", "ENTERTAINMENT"),
    ("Resort & Country Club Entry", "ENTERTAINMENT"),
    ("Steam Valve Game Purchase", "ENTERTAINMENT"),
    ("Sony PlayStation Store", "ENTERTAINMENT"),
    ("Xbox Live Game Pass", "ENTERTAINMENT"),
    ("Spotify Premium Music Sub", "ENTERTAINMENT"),
    ("Netflix Video Streaming", "ENTERTAINMENT"),
    ("Amazon Prime Video", "ENTERTAINMENT"),
    ("Disney Hotstar VIP Sub", "ENTERTAINMENT"),
    ("Zee5 Premium Streaming", "ENTERTAINMENT"),
    ("SonyLIV Monthly Plan", "ENTERTAINMENT"),
    ("BookMyShow Concert Passes", "ENTERTAINMENT"),
    ("Insider Paytm Events", "ENTERTAINMENT"),
    ("Cricket Stadium Match Ticket", "ENTERTAINMENT"),
    ("Gymkhana Club Membership", "ENTERTAINMENT"),

    # HEALTHCARE
    ("Apollo Medico Pharmacy Store", "HEALTHCARE"),
    ("Medplus Chemist & Druggist", "HEALTHCARE"),
    ("Netmeds Online Pharmacy", "HEALTHCARE"),
    ("Tata 1mg Medicines", "HEALTHCARE"),
    ("PharmEasy Healthcare Lab", "HEALTHCARE"),
    ("Dr Lal Pathlabs Blood Test", "HEALTHCARE"),
    ("Max Healthcare Diagnostics", "HEALTHCARE"),
    ("Metropolis Health Lab", "HEALTHCARE"),
    ("City Care Multi Speciality Hospital", "HEALTHCARE"),
    ("Dental Care & Root Canal Clinic", "HEALTHCARE"),
    ("Ayurvedic Pharmacy Vaidyashala", "HEALTHCARE"),
    ("Dr Batras Homeopathy Clinic", "HEALTHCARE"),
    ("Apollo Clinic Consultation", "HEALTHCARE"),
    ("Fortis Hospital OPD Bill", "HEALTHCARE"),
    ("Manipal Hospital Medicals", "HEALTHCARE"),
    ("Narayana Hrudayalaya Hospital", "HEALTHCARE"),
    ("Clove Dental Clinic", "HEALTHCARE"),
    ("Vasan Eye Care Hospital", "HEALTHCARE"),
    ("Eye Foundation Optical Care", "HEALTHCARE"),
    ("Sanjeevani Polyclinic", "HEALTHCARE"),
    ("Red Cross Blood Bank Donation", "HEALTHCARE"),
    ("Physiotherapy & Rehab Center", "HEALTHCARE"),
    ("Hearing Aid Center Care", "HEALTHCARE"),
    ("Diagnostic MRI Scan Center", "HEALTHCARE"),

    # EDUCATION
    ("Kendriya Vidyalaya School Fees", "EDUCATION"),
    ("Delhi Public School Term Fee", "EDUCATION"),
    ("Brilliant Coaching Center NEET", "EDUCATION"),
    ("Allen Career Institute Tuition", "EDUCATION"),
    ("Aakash Educational Services", "EDUCATION"),
    ("FIITJEE Entrance Coaching", "EDUCATION"),
    ("Resonance Edu Academy", "EDUCATION"),
    ("College Semester Tuition Fees", "EDUCATION"),
    ("University Exam Fee Portal", "EDUCATION"),
    ("Public Library Annual Membership", "EDUCATION"),
    ("Udemy Online Course Learning", "EDUCATION"),
    ("Coursera Learning Platform", "EDUCATION"),
    ("Unacademy Plus Subscription", "EDUCATION"),
    ("Byjus Learning Classes", "EDUCATION"),
    ("Physics Wallah PW Batch", "EDUCATION"),
    ("Vedantu Live Online Tuition", "EDUCATION"),
    ("Skillshare Creative Learning", "EDUCATION"),
    ("Oxford University Press Books", "EDUCATION"),
    ("NCERT Textbook Depot", "EDUCATION"),
    ("School Uniform & Stationery", "EDUCATION"),
    ("Music Academy Guitar Classes", "EDUCATION"),
    ("Dance Class Academy Monthly", "EDUCATION"),

    # INVESTMENT
    ("Zerodha Broking Equity Kite", "INVESTMENT"),
    ("Groww Nextbillion Mutual Fund", "INVESTMENT"),
    ("Upstox RKSV Securities", "INVESTMENT"),
    ("Angel One Broking App", "INVESTMENT"),
    ("ICICI Direct Share Trading", "INVESTMENT"),
    ("HDFC Sky Securities Invest", "INVESTMENT"),
    ("Kotak Securities Neo Trading", "INVESTMENT"),
    ("NSE Clearing Corporation", "INVESTMENT"),
    ("BSE StAR Mutual Fund", "INVESTMENT"),
    ("National Pension System NPS Trust", "INVESTMENT"),
    ("Sovereign Gold Bond RBI", "INVESTMENT"),
    ("SBI Mutual Fund SIP", "INVESTMENT"),
    ("HDFC Mutual Fund Investment", "INVESTMENT"),
    ("Nippon India MF SIP", "INVESTMENT"),
    ("Axis Mutual Fund Growth", "INVESTMENT"),
    ("Mirae Asset Mutual Fund", "INVESTMENT"),
    ("Parag Parikh Flexi Cap Fund", "INVESTMENT"),
    ("Smallcase Technologies", "INVESTMENT"),
    ("Vested Finance US Stocks", "INVESTMENT"),
    ("Fixed Deposit Interest Credit", "INVESTMENT"),
    ("Recurring Deposit Investment", "INVESTMENT"),

    # TRANSFERS
    ("UPI P2P Transfer to Suresh", "TRANSFERS"),
    ("Sent money to Ramesh Sharma", "TRANSFERS"),
    ("Transfer to Mom Savings", "TRANSFERS"),
    ("Self Account Funds Transfer", "TRANSFERS"),
    ("Received from Amit Friend", "TRANSFERS"),
    ("Payment to Landlord Rent", "TRANSFERS"),
    ("Salary Credit from Employer", "TRANSFERS"),
    ("IMPS P2A Fund Transfer", "TRANSFERS"),
    ("NEFT Remittance to Vendor", "TRANSFERS"),
    ("RTGS Fund Settlement", "TRANSFERS"),
    ("Pocket Money to Sibling", "TRANSFERS"),
    ("Splitwise Settlement Friend", "TRANSFERS"),
    ("Transferred to Rahul Verma", "TRANSFERS"),
    ("Payment to Maid Househelp", "TRANSFERS"),
    ("Cook Monthly Payment", "TRANSFERS"),

    # TRAVEL / TRANSPORT
    ("MakeMyTrip Flight Booking", "TRANSPORT"),
    ("IRCTC Indian Railways Ticket", "TRANSPORT"),
    ("Goibibo Hotel Room Stay", "TRANSPORT"),
    ("EaseMyTrip Flight Ticket", "TRANSPORT"),
    ("Cleartrip Airlines Booking", "TRANSPORT"),
    ("Vistara Airlines Flight", "TRANSPORT"),
    ("Air India Flight Ticket", "TRANSPORT"),
    ("IndiGo 6E Flight Air", "TRANSPORT"),
    ("Akasa Air Flight Booking", "TRANSPORT"),
    ("SpiceJet Airlines Journey", "TRANSPORT"),
    ("Yatra Travel & Tour Package", "TRANSPORT"),
    ("OYO Rooms Hotel Stay", "TRANSPORT"),
    ("Treebo Hotels Booking", "TRANSPORT"),
    ("FabHotels City Stay", "TRANSPORT"),
    ("Taj Hotels Palaces Resorts", "TRANSPORT"),
    ("Marriott International Stay", "TRANSPORT"),
    ("Airbnb Homestay Rental", "TRANSPORT"),
    ("RedBus Intercity Bus Ticket", "TRANSPORT"),
    ("AbhiBus Bus Reservation", "TRANSPORT"),
    ("IntrCity SmartBus Journey", "TRANSPORT"),
    ("Visa Application VFS Global", "TRANSPORT"),
    ("Passport Seva Kendra Fee", "TRANSPORT"),

    # PERSONAL
    ("Javed Habib Hair & Beauty Salon", "PERSONAL"),
    ("Looks Unisex Hair Parlour", "PERSONAL"),
    ("Enrich Beauty Salon & Spa", "PERSONAL"),
    ("Tony & Guy Hairdressing", "PERSONAL"),
    ("Geetanjali Salon Studio", "PERSONAL"),
    ("Ayur Care Spa & Massage", "PERSONAL"),
    ("Urban Company Salon at Home", "PERSONAL"),
    ("Yes Madam Salon Home Service", "PERSONAL"),
    ("Kaya Skin Clinic Laser Care", "PERSONAL"),
    ("VLCC Wellness & Slimming", "PERSONAL"),
    ("Bodycraft Salon & Clinic", "PERSONAL"),
    ("Grooming Lounge Mens Barber", "PERSONAL"),
    ("Nail Art Studio Pedicure", "PERSONAL"),
    ("Tattoo Studio Ink Parlour", "PERSONAL"),
    ("Gold Gym Fitness Membership", "PERSONAL"),
    # SALARY_INCOME
    ("Monthly Salary Credit from Employer", "SALARY_INCOME"),
    ("Salary credited for August 2026", "SALARY_INCOME"),
    ("Consulting Fee Income Received", "SALARY_INCOME"),
    ("Freelance Project Payment", "SALARY_INCOME"),
    ("Bonus Payout from Company", "SALARY_INCOME"),
    ("Stipend Credit Internship", "SALARY_INCOME"),

    # FEES_CHARGES
    ("Annual Credit Card Maintenance Fee", "FEES_CHARGES"),
    ("ATM Cash Withdrawal Charges", "FEES_CHARGES"),
    ("Bank SMS Alert Service Charges", "FEES_CHARGES"),
    ("Late Payment Surcharge Fee", "FEES_CHARGES"),
    ("Debit Card Annual Charges", "FEES_CHARGES"),
    ("Non Maintenance Minimum Balance Penalty", "FEES_CHARGES"),
    ("Cheque Bounce Charges Penalty", "FEES_CHARGES"),

    # OTHERS
    ("Miscellaneous Expense Payment", "OTHERS"),
    ("General Vendor Settlement", "OTHERS"),
    ("Unclassified Store Outlet", "OTHERS"),
    ("Misc Payment Service", "OTHERS")
]


def clean_text(text: str) -> str:
    t = text.lower()
    t = re.sub(r"[^a-z0-9\s]", " ", t)
    return re.sub(r"\s+", " ", t).strip()


def train_and_export():
    texts = [clean_text(x[0]) for x in DATASET]
    labels = [x[1] for x in DATASET]

    label_to_idx = {cat: i for i, cat in enumerate(CATEGORIES)}
    y = np.array([label_to_idx[l] for l in labels])

    # Combine word n-grams (1-2 words) and character subword n-grams (3-4 chars)
    word_vectorizer = TfidfVectorizer(
        ngram_range=(1, 2),
        min_df=1,
        max_features=1200,
        sublinear_tf=True
    )

    char_vectorizer = TfidfVectorizer(
        analyzer="char_wb",
        ngram_range=(3, 4),
        min_df=1,
        max_features=1500,
        sublinear_tf=True
    )

    feature_union = FeatureUnion([
        ("word", word_vectorizer),
        ("char", char_vectorizer)
    ])

    X = feature_union.fit_transform(texts)

    # Train multinomial logistic regression
    clf = LogisticRegression(
        C=2.5,
        max_iter=500,
        solver="lbfgs",
        class_weight="balanced",
        random_state=42
    )
    clf.fit(X, y)

    # Export vocabulary and IDF weights
    word_vocab = {str(k): int(v) for k, v in word_vectorizer.vocabulary_.items()}
    word_idf = [float(x) for x in word_vectorizer.idf_.tolist()]
    char_vocab = {str(k): int(v) for k, v in char_vectorizer.vocabulary_.items()}
    char_idf = [float(x) for x in char_vectorizer.idf_.tolist()]

    # Coefficients shape: (n_classes, n_features)
    # Intercept shape: (n_classes,)
    weights = [[float(c) for c in row] for row in clf.coef_.tolist()]
    intercept = [float(b) for b in clf.intercept_.tolist()]

    clf_classes = [CATEGORIES[int(i)] for i in clf.classes_]

    model_payload = {
        "categories": clf_classes,
        "word_vocab": word_vocab,
        "word_idf": word_idf,
        "char_vocab": char_vocab,
        "char_idf": char_idf,
        "weights": weights,
        "intercept": intercept,
        "version": "1.0.0",
        "timestamp": 1787722000000
    }

    output_dir = "app/src/main/assets/models"
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "merchant_classifier_weights.json")

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(model_payload, f, separators=(",", ":"))

    file_size_kb = os.path.getsize(output_path) / 1024.0
    print(f"Model successfully trained & exported to {output_path} ({file_size_kb:.2f} KB)")


if __name__ == "__main__":
    train_and_export()
