import PyPDF2
import sys

def read_pdf(file_path):
    try:
        with open(file_path, 'rb') as file:
            reader = PyPDF2.PdfReader(file)
            text = ""
            for page in reader.pages:
                text += page.extract_text() + "\n"
            with open("pdf_content.txt", "w", encoding="utf-8") as out_file:
                out_file.write(text)
    except Exception as e:
        print(f"Error reading PDF: {e}", file=sys.stderr)

if __name__ == "__main__":
    read_pdf(r"C:\Users\Admin\Downloads\Product Backend - Technical Assessment GI Autumn 2026.pdf")
