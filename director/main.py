import os, subprocess

def download(url):
	return url

def main():
	# TODO: Check input
	# Input file contains a URL

	with open('output.csv', 'w') as out:
		pass

	with open('input.txt') as f:
		downloaded_file = download(f.read().strip())

	# Call ems-processor on downloaded file and created csv file
	subprocess.call(['java', '-jar', '../out/artifacts/ems_processor_jar/ems-processor.jar', downloaded_file, 'output.csv'])

	# Upload to Google Calendar?
	#
	#

	os.remove(downloaded_file)


if __name__ == '__main__':
	main()