from flask import Flask, request, jsonify
import torch
from transformers import BlipProcessor, BlipForQuestionAnswering
from PIL import Image
import base64
import io
import logging
import time
import os
from typing import Dict, List, Tuple
import threading
from concurrent.futures import ThreadPoolExecutor

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

class OptimizedVeterinaryVQAService:
    def __init__(self):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        logger.info(f"Using device: {self.device}")
        
        # Optimize CUDA settings
        if torch.cuda.is_available():
            torch.backends.cudnn.benchmark = True
            torch.backends.cudnn.enabled = True
        
        # Load models with optimizations
        self.load_models()
        
        # Larger thread pool for parallel processing
        self.executor = ThreadPoolExecutor(max_workers=8)
        
        # Pre-compiled question sets with smart prioritization
        self.critical_questions = [
            "Are there any visible wounds or bleeding on the animal?",
            "Does the animal appear to be in severe distress or pain?",
            "Does the animal appear unconscious or unresponsive?",
        ]
        
        self.priority_questions = [
            "Are there any swollen areas on the animal's body?",
            "Is there any discharge from the eyes, nose, or ears?",
            "Does the animal appear lethargic or very weak?",
            "Is the animal limping or favoring one leg?",
        ]
        
        self.quick_health_questions = [
            "Are the animal's eyes clear and bright?",
            "Does the animal appear alert and responsive?",
            "Does the animal's coat appear healthy?",
        ]
        
        # Cache for processed inputs to avoid reprocessing
        self._input_cache = {}
        self._cache_lock = threading.Lock()
    
    def load_models(self):
        try:
            logger.info("Loading BLIP VQA model...")
            
            # Clear cache to avoid CUDA issues
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            
            self.blip_processor = BlipProcessor.from_pretrained(
                "Salesforce/blip-vqa-base",
                cache_dir="./model_cache"  # Local cache
            )
            
            self.blip_model = BlipForQuestionAnswering.from_pretrained(
                "Salesforce/blip-vqa-base",
                torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
                cache_dir="./model_cache"
            )
            
            # Move to device before any operations
            self.blip_model.to(self.device)
            self.blip_model.eval()
            
            logger.info(f"Model loaded on device: {self.device}")
            logger.info(f"Model dtype: {self.blip_model.dtype}")
            
        except Exception as e:
            logger.error(f"Failed to load models: {e}")
            # Try CPU fallback
            if "cuda" in str(e).lower():
                logger.info("Falling back to CPU...")
                self.device = "cpu"
                self.load_models()  # Retry with CPU
            else:
                raise

    def _preprocess_image_batch(self, image: Image.Image, questions: List[str]) -> Dict:
        """Preprocess image for batch processing - fixed version"""
        try:
            # Create a unique key for caching
            image_hash = hash(image.tobytes())
            questions_key = hash(tuple(questions))
            cache_key = (image_hash, questions_key)
            
            with self._cache_lock:
                if cache_key in self._input_cache:
                    return self._input_cache[cache_key]
            
            # Process each question individually (more reliable)
            processed_inputs = {}
            for question in questions:
                try:
                    inputs = self.blip_processor(image, question, return_tensors="pt")
                    if torch.cuda.is_available():
                        inputs = {k: v.to(self.device) for k, v in inputs.items()}
                    processed_inputs[question] = inputs
                except Exception as e:
                    logger.error(f"Error preprocessing question '{question}': {e}")
                    # Create empty inputs as fallback
                    processed_inputs[question] = None
            
            with self._cache_lock:
                self._input_cache[cache_key] = processed_inputs
                # Limit cache size
                if len(self._input_cache) > 10:
                    oldest_key = next(iter(self._input_cache))
                    del self._input_cache[oldest_key]

            return processed_inputs
            
        except Exception as e:
            logger.error(f"Error in _preprocess_image_batch: {e}")
            return {q: None for q in questions}
        
    def answer_questions_batch(self, image: Image.Image, questions: List[str]) -> List[Tuple[str, str, float]]:
        """Answer multiple questions with better error handling"""
        try:
            results = []
            
            # Process questions sequentially for better reliability
            for question in questions:
                try:
                    # Process each question individually
                    inputs = self.blip_processor(image, question, return_tensors="pt")
                    if torch.cuda.is_available():
                        inputs = {k: v.to(self.device) for k, v in inputs.items()}
                    
                    answer, confidence = self._answer_single_optimized(inputs, question)
                    results.append((question, answer, confidence))
                    
                except Exception as e:
                    logger.error(f"Error processing question '{question}': {e}")
                    results.append((question, "unknown", 0.0))
            
            return results
                
        except Exception as e:
            logger.error(f"Error in batch processing: {e}")
            return [(q, "unknown", 0.0) for q in questions]
    
    def _answer_single_optimized(self, processed_inputs: Dict, question: str) -> Tuple[str, float]:
        """ Single question answering with better error handling"""
        try:
            if processed_inputs is None:
                return "unknown", 0.0
                
            with torch.no_grad():
                # Use torch.cuda.amp for mixed precision if available
                if torch.cuda.is_available():
                    with torch.cuda.amp.autocast():
                        outputs = self.blip_model.generate(
                            **processed_inputs, 
                            max_length=20,
                            num_beams=2,
                            do_sample=False,
                            early_stopping=True
                        )
                else:
                    outputs = self.blip_model.generate(
                        **processed_inputs, 
                        max_length=20,
                        num_beams=2,
                        do_sample=False,
                        early_stopping=True
                    )
                
                answer = self.blip_processor.decode(outputs[0], skip_special_tokens=True)
        
            # Quick confidence calculation
            confidence = self._calculate_confidence_fast(answer, question)
            
            return answer.strip(), confidence
            
        except Exception as e:
            logger.error(f"Error in optimized answer for '{question}': {e}")
            return "unknown", 0.0
    
    def _calculate_confidence_fast(self, answer: str, question: str) -> float:
        """Fast confidence calculation using lookup tables"""
        answer_lower = answer.lower()
        
        # Pre-computed confidence mappings for speed
        if answer_lower in {'yes', 'no'}:
            return 0.85
        elif answer_lower in {'healthy', 'normal', 'clear', 'good'}:
            return 0.80
        elif answer_lower in {'visible', 'appears', 'seems'}:
            return 0.70
        elif any(word in answer_lower for word in ['maybe', 'possibly', 'unclear', 'unknown']):
            return 0.3
        else:
            return 0.6
    
    def analyze_image_fast(self, image: Image.Image) -> Dict:
        """Fast comprehensive analysis with smart prioritization"""
        results = {
            'critical_findings': [],
            'priority_findings': [],
            'health_findings': [],
            'overall_assessment': {},
            'processing_time': 0,
            'model_used': 'BLIP'
        }
        
        start_time = time.time()
        
        try:
            # Phase 1: Critical questions first (parallel)
            logger.info("Phase 1: Checking critical conditions...")
            critical_results = self.answer_questions_batch(image, self.critical_questions)
            
            critical_concerns_found = False
            for question, answer, confidence in critical_results:
                result = {
                    'question': question,
                    'answer': answer,
                    'confidence': confidence,
                    'is_concerning': self._is_concerning_answer_fast(answer, question, confidence)
                }
                results['critical_findings'].append(result)
                
                if result['is_concerning'] and confidence > 0.6:
                    critical_concerns_found = True
                    logger.warning(f"Critical issue: {question} -> {answer}")
            
            # Phase 2: Priority questions (only if no critical issues or run in parallel)
            logger.info("Phase 2: Checking priority conditions...")
            priority_results = self.answer_questions_batch(image, self.priority_questions)
            
            for question, answer, confidence in priority_results:
                result = {
                    'question': question,
                    'answer': answer,
                    'confidence': confidence,
                    'is_concerning': self._is_concerning_answer_fast(answer, question, confidence)
                }
                results['priority_findings'].append(result)
            
            # Phase 3: Quick health check (abbreviated if critical issues found)
            if not critical_concerns_found:
                logger.info("Phase 3: Quick health assessment...")
                health_results = self.answer_questions_batch(image, self.quick_health_questions)
                
                for question, answer, confidence in health_results:
                    result = {
                        'question': question,
                        'answer': answer,
                        'confidence': confidence,
                        'is_positive': self._is_positive_answer_fast(answer)
                    }
                    results['health_findings'].append(result)
            
            # Generate assessment
            results['overall_assessment'] = self._generate_assessment_fast(results)
            
        except Exception as e:
            logger.error(f"Error during fast analysis: {e}")
            results['error'] = str(e)
        
        finally:
            results['processing_time'] = time.time() - start_time
            logger.info(f"Fast analysis completed in {results['processing_time']:.2f} seconds")
        
        return results
    
    def _is_concerning_answer_fast(self, answer: str, question: str, confidence: float) -> bool:
        """Fast concerning answer detection"""
        if confidence < 0.4:
            return False
        
        answer_lower = answer.lower()
        # Quick keyword matching
        if 'yes' in answer_lower and any(word in question.lower() for word in ['wound', 'bleeding', 'distress', 'swollen', 'discharge']):
            return True
        if 'no' in answer_lower and any(word in question.lower() for word in ['clear', 'healthy', 'alert']):
            return True
        
        return False
    
    def _is_positive_answer_fast(self, answer: str) -> bool:
        """Fast positive answer detection"""
        return any(word in answer.lower() for word in ['yes', 'healthy', 'normal', 'clear', 'good', 'bright'])
    
    def _generate_assessment_fast(self, results: Dict) -> Dict:
        """Fast assessment generation"""
        critical_concerns = sum(1 for r in results['critical_findings'] if r['is_concerning'])
        priority_concerns = sum(1 for r in results['priority_findings'] if r['is_concerning'])
        positive_findings = sum(1 for r in results['health_findings'] if r['is_positive'])
        
        if critical_concerns > 0:
            urgency = 'CRITICAL'
            condition = f"Critical veterinary concerns detected"
            recommendations = ["Seek immediate veterinary attention"]
        elif priority_concerns > 1:
            urgency = 'HIGH'
            condition = f"Multiple health concerns detected"
            recommendations = ["Schedule veterinary consultation within 24 hours"]
        elif priority_concerns > 0:
            urgency = 'MEDIUM'
            condition = f"Health concern detected"
            recommendations = ["Schedule veterinary consultation within 48 hours"]
        else:
            urgency = 'LOW'
            condition = "Animal appears healthy"
            recommendations = ["Continue regular care"]
        
        return {
            'urgency_level': urgency,
            'condition': condition,
            'confidence': 0.8 if critical_concerns or priority_concerns else 0.7,
            'recommendations': recommendations,
            'summary': f"Critical: {critical_concerns}, Priority: {priority_concerns}, Positive: {positive_findings}"
        }

# Global service instance
vqa_service = None

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'device': vqa_service.device if vqa_service else 'unknown',
        'models_loaded': vqa_service is not None,
        'optimization': 'enabled'
    })

@app.route('/analyze', methods=['POST'])
def analyze_image():
    """Optimized analysis endpoint"""
    try:
        if 'image' not in request.files and 'image_base64' not in request.json:
            return jsonify({'error': 'No image provided'}), 400
        
        # Get image from request
        if 'image' in request.files:
            image_file = request.files['image']
            image = Image.open(image_file.stream).convert('RGB')
        else:
            # Handle base64 image
            image_data = base64.b64decode(request.json['image_base64'])
            image = Image.open(io.BytesIO(image_data)).convert('RGB')
        
        # Resize image for faster processing (optional)
        max_size = (512, 512)
        if image.size[0] > max_size[0] or image.size[1] > max_size[1]:
            image.thumbnail(max_size, Image.Resampling.LANCZOS)
        
        # Perform fast analysis
        results = vqa_service.analyze_image_fast(image)
        
        return jsonify({
            'success': True,
            'results': results,
            'timestamp': time.time()
        })
        
    except Exception as e:
        logger.error(f"Analysis error: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/quick-question', methods=['POST'])
def quick_question():
    """Optimized single question endpoint"""
    try:
        data = request.get_json()
        
        if 'image_base64' not in data or 'question' not in data:
            return jsonify({'error': 'Missing image_base64 or question'}), 400
        
        # Decode image
        image_data = base64.b64decode(data['image_base64'])
        image = Image.open(io.BytesIO(image_data)).convert('RGB')
        
        # Resize for speed
        max_size = (512, 512)
        if image.size[0] > max_size[0] or image.size[1] > max_size[1]:
            image.thumbnail(max_size, Image.Resampling.LANCZOS)
        
        # Answer single question
        results = vqa_service.answer_questions_batch(image, [data['question']])
        question, answer, confidence = results[0]
        
        return jsonify({
            'success': True,
            'question': question,
            'answer': answer,
            'confidence': confidence
        })
        
    except Exception as e:
        logger.error(f"Quick question error: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

def initialize_service():
    """Initialize the optimized VQA service"""
    global vqa_service
    try:
        logger.info("Initializing optimized VQA service...")
        vqa_service = OptimizedVeterinaryVQAService()
        logger.info("Optimized VQA service initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize VQA service: {e}")
        raise

if __name__ == '__main__':
    # Initialize service
    initialize_service()
    
    # Start Flask app
    port = int(os.environ.get('PORT', 5000))
    host = os.environ.get('HOST', '127.0.0.1')
    
    logger.info(f"Starting optimized VQA service on {host}:{port}")
    app.run(host=host, port=port, debug=False, threaded=True)