from pathlib import Path
from openpyxl import Workbook
from openpyxl.styles import Font

OUTPUT = Path(__file__).resolve().parent / "modelo_cadastro_ocs.xlsx"

COLUNAS = [
    "ocs_nome", "ocs_inscricao_federal", "ocs_endereco", "ocs_endereco_numero",
    "ocs_endereco_bairro", "ocs_endereco_cidade", "ocs_endereco_uf",
    "ocs_endereco_cep", "ocs_contato_nome", "ocs_contato_telefone",
]

EXEMPLOS = [
    ["CARDIOVITA SERVIÇOS MÉDICOS LTDA", "18674947000224", "Rua Capitão Carlos de Moura", "385",
     "Vila Pantaleão", "Caçapava", "SP", "12280050", "", "1236255433"],
    ["CENTROCOR - CENTRO DE DIAGNÓSTICO E CIRURGIA DO CORAÇÃO LTDA", "53325148000162",
     "Avenida Doutor Adhemar de Barros", "306", "Jardim São Dimas", "São José dos Campos", "SP",
     "12245011", "", "1239231755"],
]

wb = Workbook()
sheet = wb.active
sheet.title = "Cadastro OCS"

sheet.append(COLUNAS)
for celula in sheet[1]:
    celula.font = Font(bold=True)

for linha in EXEMPLOS:
    sheet.append(linha)

larguras = [45, 20, 35, 12, 25, 25, 6, 12, 25, 18]
for indice, largura in enumerate(larguras, start=1):
    sheet.column_dimensions[sheet.cell(row=1, column=indice).column_letter].width = largura

wb.save(OUTPUT)
print(f"Salvo em {OUTPUT}")
